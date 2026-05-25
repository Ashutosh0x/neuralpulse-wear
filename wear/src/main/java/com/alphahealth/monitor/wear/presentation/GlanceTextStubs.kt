package androidx.wear.glance.text

import androidx.compose.runtime.Composable
import androidx.wear.glance.unit.ColorProvider

@Composable
fun Text(
    text: String,
    style: TextStyle = TextStyle()
) {
    // Stub
}

data class TextStyle(
    val color: ColorProvider? = null,
    val fontSize: Any? = null
)
