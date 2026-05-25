package com.alphahealth.monitor.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alphahealth.monitor.ui.theme.AppTypography
import com.alphahealth.monitor.ui.theme.LocalNeuralPulseColors
import com.alphahealth.monitor.ui.theme.darkNeuralPulseColors
import com.alphahealth.monitor.ui.theme.lightNeuralPulseColors

// NeuralPulse Premium Color Palette
val AlphaBlack = Color(0xFF000000)
val AlphaCardSurface = Color(0xFF0B0B0C)
val AlphaAccentTeal = Color(0xFF00BFA5)
val AlphaAccentBlue = Color(0xFF3B82F6)
val AlphaMintGreen = Color(0xFF10B981)
val AlphaWarningAmber = Color(0xFFF59E0B)
val AlphaMutedRuby = Color(0x26EF4444)
val AlphaTextPrimary = Color(0xFFFFFFFF)
val AlphaTextSecondary = Color(0xFF9CA3AF)

@Composable
fun NeuralPulseTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = AlphaAccentTeal,
            secondary = AlphaMintGreen,
            tertiary = AlphaAccentBlue,
            error = Color(0xFFF87171),
            background = AlphaBlack,
            surface = AlphaCardSurface,
            surfaceVariant = Color(0xFF1E1F20),
            surfaceContainerLowest = Color(0xFF0E0E0E),
            surfaceContainerLow = Color(0xFF1B1B1B),
            surfaceContainer = Color(0xFF1E1F20),
            surfaceContainerHigh = Color(0xFF282A2C),
            surfaceContainerHighest = Color(0xFF333537),
            onBackground = AlphaTextPrimary,
            onSurface = AlphaTextPrimary,
            onSurfaceVariant = AlphaTextSecondary,
            outline = Color(0xFF8E918F),
            outlineVariant = Color(0xFF444746),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF00897B),
            secondary = AlphaMintGreen,
            tertiary = AlphaAccentBlue,
            error = Color(0xFFEF4444),
            background = Color(0xFFF3F4F6),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE1E3E1),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF8FAFD),
            surfaceContainer = Color(0xFFF0F4F9),
            surfaceContainerHigh = Color(0xFFE9EEF6),
            surfaceContainerHighest = Color(0xFFDDE3EA),
            onBackground = Color(0xFF111827),
            onSurface = Color(0xFF111827),
            onSurfaceVariant = Color(0xFF4B5563),
            outline = Color(0xFF747775),
            outlineVariant = Color(0xFFC4C7C5),
        )
    }

    val customColorsPalette = if (darkTheme) darkNeuralPulseColors else lightNeuralPulseColors

    CompositionLocalProvider(LocalNeuralPulseColors provides customColorsPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

@Composable
fun NeuralPulseDataCard(
    title: String,
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp), // Enforces official One UI 6 structural rounding
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "$title Icon",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun TelemetryGridItem(
    label: String,
    value: String,
    valueColor: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            fontFamily = FontFamily.Monospace, // Prevents jitter during live numeric tracking updates
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
