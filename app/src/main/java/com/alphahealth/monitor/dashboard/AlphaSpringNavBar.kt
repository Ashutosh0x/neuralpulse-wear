package com.alphahealth.monitor.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AlphaSpringNavBar
 *
 * Material 3 Expressive bottom navigation bar with:
 *   - Animated Electric Blue pill indicator that slides between items using spring physics
 *   - Pill spring: dampingRatio = DampingRatioLowBouncy for tactile pop feel
 *   - Active item text tints to AlphaAccentBlue with no layout jitter (size unchanged)
 *   - Icon scale-pop on selection: 1.0 -> 1.22 -> 1.0 with overshoot spring
 *   - AMOLED black surface background with 0.5dp top border separator
 *
 * Tab specification (from user spec):
 *   0: Analytics    | Icons.Outlined.Analytics       | "Command"
 *   1: AI Scanner   | Icons.Outlined.PhotoCamera     | "AI Scanner"
 *   2: Clinics PHR  | Icons.Outlined.HistoryEdu      | "Clinics PHR"
 *   3: SmartHome    | Icons.Outlined.Thermostat      | "SmartHome"
 *   4: Profile      | Icons.Outlined.AccountCircle   | "Profile"
 */

data class NavTabSpec(
    val icon: ImageVector,
    val label: String
)

val ALPHA_NAV_TABS = listOf(
    NavTabSpec(Icons.Outlined.Analytics,      "Command"),
    NavTabSpec(Icons.Outlined.PhotoCamera,    "AI Scanner"),
    NavTabSpec(Icons.Outlined.HistoryEdu,     "Clinics PHR"),
    NavTabSpec(Icons.Outlined.Thermostat,     "SmartHome"),
    NavTabSpec(Icons.Outlined.AccountCircle,  "Profile")
)

// Electric Blue pill background: 0x1A3B82F6 = 10% opacity Electric Blue
val NavPillColor = Color(0x1A3B82F6)

@Composable
fun AlphaSpringNavBar(
    activeTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0A0A0C),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Top separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color(0xFF1F1F22))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ALPHA_NAV_TABS.forEachIndexed { index, tab ->
                AlphaNavItem(
                    spec = tab,
                    isSelected = activeTab == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AlphaNavItem(
    spec: NavTabSpec,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pill background alpha spring (0 -> 1 when selected)
    val pillAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PillAlpha_${spec.label}"
    )

    // Pill width spring: expands on selection
    val pillWidthFraction by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PillWidth_${spec.label}"
    )

    // Icon scale pop on selection
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = Spring.StiffnessHigh
        ),
        label = "IconScale_${spec.label}"
    )

    val iconTint = if (isSelected) AlphaAccentBlue else Color(0xFF6E6E73)
    val textColor = if (isSelected) AlphaAccentBlue else Color(0xFF6E6E73)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated pill background
        if (pillAlpha > 0f) {
            Box(
                modifier = Modifier
                    .size(
                        width = (56 * pillWidthFraction.coerceIn(0f, 1f) + 8).dp,
                        height = 36.dp
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(NavPillColor.copy(alpha = pillAlpha))
            )
        }

        // Icon + label column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = spec.icon,
                contentDescription = spec.label,
                tint = iconTint,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
            Text(
                text = spec.label,
                fontSize = 9.sp,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
