package androidx.wear.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.wear.glance.unit.ColorProvider

abstract class GlanceAppWidget {
    @Composable
    abstract fun Content(context: Context, glanceId: Any)
}

interface GlanceModifier {
    fun background(colorProvider: ColorProvider): GlanceModifier = this
    fun padding(dp: Dp): GlanceModifier = this

    companion object : GlanceModifier
}

object GlanceAlignment {
    object Horizontal {
        val CenterHorizontally = Any()
    }
}
