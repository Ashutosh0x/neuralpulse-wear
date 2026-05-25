package androidx.room

import android.content.Context
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Entity(val tableName: String = "")

@Target(AnnotationTarget.FIELD)
annotation class PrimaryKey(val autoGenerate: Boolean = false)

@Target(AnnotationTarget.FIELD)
annotation class ColumnInfo(val name: String = "")

@Target(AnnotationTarget.CLASS)
annotation class Dao

@Target(AnnotationTarget.FUNCTION)
annotation class Insert(val onConflict: Int = OnConflictStrategy.ABORT)

@Target(AnnotationTarget.FUNCTION)
annotation class Query(val value: String)

@Target(AnnotationTarget.CLASS)
annotation class Database(
    val entities: Array<KClass<*>>,
    val version: Int,
    val exportSchema: Boolean = true
)

object OnConflictStrategy {
    const val REPLACE = 1
    const val ABORT = 2
}

abstract class RoomDatabase

object Room {
    fun <T : RoomDatabase> databaseBuilder(
        context: Context,
        klass: Class<T>,
        name: String
    ): Builder<T> {
        return Builder(klass)
    }

    class Builder<T : RoomDatabase>(private val klass: Class<T>) {
        fun fallbackToDestructiveMigration(): Builder<T> = this
        fun build(): T {
            // Instantiate the abstract class via reflection or return a proxy
            // Since it's a stub, we can just return a simple instanced mock or use a proxy
            val ctor = klass.getDeclaredConstructor()
            ctor.isAccessible = true
            return ctor.newInstance()
        }
    }
}
