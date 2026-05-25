package com.alphahealth.monitor.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// One UI 6 Premium Color Palette
val AlphaBlack = Color(0xFF000000)
val AlphaCardSurface = Color(0xFF0B0B0C)
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
            primary = AlphaAccentBlue,
            secondary = AlphaMintGreen,
            error = Color(0xFFF87171),
            background = AlphaBlack,
            surface = AlphaCardSurface,
            onBackground = AlphaTextPrimary,
            onSurface = AlphaTextPrimary,
            onSurfaceVariant = AlphaTextSecondary
        )
    } else {
        lightColorScheme(
            primary = AlphaAccentBlue,
            secondary = AlphaMintGreen,
            error = Color(0xFFEF4444),
            background = Color(0xFFF3F4F6), // Soft grey background
            surface = Color(0xFFFFFFFF),    // Clean white surface card
            onBackground = Color(0xFF111827),
            onSurface = Color(0xFF111827),
            onSurfaceVariant = Color(0xFF4B5563)
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
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
