package androidx.wear.glance.appwidget.layout

import androidx.compose.runtime.Composable
import androidx.wear.glance.GlanceModifier

@Composable
fun Box(
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable () -> Unit
) {
    content()
}

@Composable
fun Column(
    horizontalAlignment: Any? = null,
    content: @Composable () -> Unit
) {
    content()
}
