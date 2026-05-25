package com.alphahealth.monitor.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Entity(tableName = "watch_telemetry_metrics")
data class WatchTelemetryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "eda_value") val edaValue: Float,
    @ColumnInfo(name = "heart_rate") val heartRate: Int,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)

@Dao
interface TelemetryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchedMetrics(metrics: List<WatchTelemetryEntity>)
    
    @Query("SELECT * FROM watch_telemetry_metrics ORDER BY timestamp DESC LIMIT 100")
    suspend fun getRecentMetrics(): List<WatchTelemetryEntity>
}

@Database(entities = [WatchTelemetryEntity::class], version = 1, exportSchema = false)
abstract class TelemetryDatabase : RoomDatabase() {
    
    abstract fun telemetryDao(): TelemetryDao

    companion object {
        @Volatile
        private var INSTANCE: TelemetryDatabase? = null

        fun getDatabase(context: Context): TelemetryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TelemetryDatabase::class.java,
                    "alpha_telemetry_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Helper Repository class showing performance-centric transactional batch writing
class TelemetryRepository(private val telemetryDao: TelemetryDao) {

    // Cache buffer holding biometrics in-memory
    private val memoryBuffer = mutableListOf<WatchTelemetryEntity>()
    private val batchLimit = 100

    /**
     * Buffers incoming telemetry points to avoid disk bottlenecks.
     * Commits a transactional block of 100 inputs directly onto Dispatchers.IO context.
     */
    suspend fun bufferWatchTelemetry(eda: Float, hr: Int) = withContext(Dispatchers.IO) {
        val entry = WatchTelemetryEntity(
            edaValue = eda,
            heartRate = hr,
            timestamp = System.currentTimeMillis()
        )
        memoryBuffer.add(entry)

        if (memoryBuffer.size >= batchLimit) {
            val batchToCommit = ArrayList(memoryBuffer)
            memoryBuffer.clear()
            
            // Execute batch transaction insert
            telemetryDao.insertBatchedMetrics(batchToCommit)
            LogMock.d("TelemetryRepository", "Disk Sync: Completed Room database batch transaction for 100 entries.")
        }
    }
}

object LogMock {
    fun d(tag: String, msg: String) {
        android.util.Log.d(tag, msg)
    }
}
