package com.alphahealth.monitor.wear.presentation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.wear.glance.GlanceAppWidget
import androidx.wear.glance.GlanceModifier
import androidx.wear.glance.GlanceAlignment
import androidx.wear.glance.appwidget.layout.Box
import androidx.wear.glance.appwidget.layout.Column
import androidx.wear.glance.layout.fillMaxSize
import androidx.wear.glance.text.Text
import androidx.wear.glance.text.TextStyle
import androidx.wear.glance.unit.ColorProvider

class NeuralPulseGlanceWidget : GlanceAppWidget() {

    @Composable
    override fun Content(context: Context, glanceId: Any) {
        // Enforces AMOLED pure black constraint natively inside the RemoteCompose window canvas
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(android.graphics.Color.BLACK))
                .padding(8.dp)
        ) {
            Column(horizontalAlignment = GlanceAlignment.Horizontal.CenterHorizontally) {
                Text(
                    text = "RECOVERY INDEX",
                    style = TextStyle(color = ColorProvider(0xFF9CA3AF)) // Muted slate gray
                )
                Text(
                    text = "88%", 
                    style = TextStyle(
                        color = ColorProvider(0xFF10B981), // Mint Green optimized state
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                    )
                )
            }
        }
    }
}
