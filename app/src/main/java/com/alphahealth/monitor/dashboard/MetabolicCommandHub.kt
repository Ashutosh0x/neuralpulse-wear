package com.alphahealth.monitor.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle

import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════════════
//  ALPHAHEALTH METABOLIC COMMAND HUB
//  ──────────────────────────────────
//  A single, production-ready Jetpack Compose screen implementing the
//  clinical-grade metabolic dashboard from the design specification.
//
//  Components:
//    1. MetabolicCommandHubScreen  — Root scaffold with nav bar
//    2. HubStatusBar               — Time + "Store Online" pill
//    3. HubGlobalHeader            — Title + hamburger icon
//    4. MetabolicGaugeCard         — Canvas speedometer arc
//    5. DailyStepVigilanceCard     — Circular progress + step data
//    6. ConsistencyCommandCard     — Weekly streak chips + insight
//    7. HubBottomNavBar            — 4 tabs + floating center button
//    8. HubFooterCaption           — Technical ecosystem text
// ═══════════════════════════════════════════════════════════════════════════

// ── Color Constants ─────────────────────────────────────────────────────────
private val ScreenBlack           = Color(0xFF000000)
private val CardSlate             = Color(0xFF131517)
private val EmeraldMint           = Color(0xFF10B981)
private val WarningAmber          = Color(0xFFF59E0B)
private val ClearBlue             = Color(0xFF3B82F6)
private val SecondaryGray         = Color(0xFF9CA3AF)
private val DeepForest            = Color(0xFF064E3B)
private val PillGreenBg           = Color(0xFF0D3B2E)
private val MutedGreenBg          = Color(0xFF14532D)
private val ChipActiveBg         = Color(0xFF1A2E23)
private val ChipInactiveBg       = Color(0xFF1C1C1E)
private val DarkCharcoal          = Color(0xFF1A1A1C)
private val GaugeTrackGray        = Color(0xFF2A2A2E)
private val InsightBg             = Color(0xFF0F1012)
private val NavBarBg              = Color(0xFF0A0A0C)
private val NavInactive           = Color(0xFF6E6E73)

// ── Monospaced Font Reference ───────────────────────────────────────────────
private val MonoFont = FontFamily.Monospace

// ═══════════════════════════════════════════════════════════════════════════
//  ROOT SCREEN
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun MetabolicCommandHubScreen() {
    Scaffold(
        containerColor = ScreenBlack,
        bottomBar = { HubBottomNavBar() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Status bar ──
            item { HubStatusBar() }

            // ── Global header ──
            item { HubGlobalHeader() }

            // ── Card 1: Metabolic Gauge ──
            item { MetabolicGaugeCard(currentValue = 72, maxValue = 100) }

            // ── Card 2: Daily Step Vigilance ──
            item {
                DailyStepVigilanceCard(
                    steps = 8412,
                    goal = 10000,
                    distanceKm = 6.2f,
                    activeMinutes = 105
                )
            }

            // ── Card 3: Consistency Command ──
            item { ConsistencyCommandCard() }

            // ── Footer caption ──
            item { HubFooterCaption() }

            // ── Bottom spacer ──
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  A. STATUS BAR & GLOBAL HEADER
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun HubStatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time
        Text(
            text = "16:29",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MonoFont
        )

        // "Store Online" pill
        Surface(
            color = DeepForest,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Store Online",
                color = EmeraldMint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun HubGlobalHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Title centered
        Text(
            text = "ALPHAHEALTH\nMETABOLIC COMMAND HUB",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        // Hamburger menu icon — top right
        IconButton(
            onClick = { /* drawer toggle */ },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  B. METABOLIC ACCELERATION HERO GAUGE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MetabolicGaugeCard(currentValue: Int, maxValue: Int) {
    val sweepFraction = currentValue.toFloat() / maxValue.toFloat()

    // Animate gauge on appear
    val animatedSweep = remember { Animatable(0f) }
    LaunchedEffect(currentValue) {
        animatedSweep.animateTo(
            targetValue = sweepFraction,
            animationSpec = tween(1400, easing = FastOutSlowInEasing)
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EmeraldMint.copy(alpha = 0.08f),
                            WarningAmber.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0.2f),
                        radius = 600f
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Gauge + labels
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Side labels
                    Text(
                        text = "MUTE",
                        color = SecondaryGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp)
                            .offset(y = (-10).dp)
                    )
                    Text(
                        text = "AMEE",
                        color = SecondaryGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .offset(y = (-10).dp)
                    )

                    // Canvas speedometer
                    SpeedometerGauge(
                        fraction = animatedSweep.value,
                        modifier = Modifier.size(200.dp)
                    )

                    // Center text overlay
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.offset(y = 10.dp)
                    ) {
                        // "72 /100"
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = Color.White,
                                        fontSize = 56.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = MonoFont
                                    )
                                ) { append("$currentValue") }
                                withStyle(
                                    SpanStyle(
                                        color = SecondaryGray,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Normal,
                                        fontFamily = MonoFont
                                    )
                                ) { append("/$maxValue") }
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "METABOLIC ACCELERATION",
                            color = SecondaryGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // "OPTIMAL ZONE" pill
                Surface(
                    color = MutedGreenBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "OPTIMAL ZONE",
                        color = EmeraldMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Custom Canvas speedometer arc gauge.
 *
 * Draws a half-circle (180°) arc with:
 * - Dark gray base track
 * - Gradient sweep from Emerald Mint → Amber → Orange
 * - Tick marks around the perimeter
 * - Glowing needle indicator at the current position
 */
@Composable
private fun SpeedometerGauge(fraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        val thinStroke = 2.dp.toPx()
        val padding = strokeWidth + 8.dp.toPx()
        val arcSize = Size(size.width - padding * 2, size.height - padding * 2)
        val arcTopLeft = Offset(padding, padding - arcSize.height * 0.1f)

        // Start angle: 180° (left), sweep: 180° (half circle)
        val startAngle = 180f
        val totalSweep = 180f

        // ── Base track (dark gray) ──
        drawArc(
            color = GaugeTrackGray,
            startAngle = startAngle,
            sweepAngle = totalSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // ── Gradient sweep arc ──
        val gradientBrush = Brush.sweepGradient(
            0.0f to EmeraldMint,
            0.25f to EmeraldMint,
            0.40f to Color(0xFF34D399),
            0.55f to Color(0xFFFBBF24),
            0.70f to WarningAmber,
            0.85f to Color(0xFFF97316),
            1.0f to Color(0xFFEF4444)
        )

        drawArc(
            brush = gradientBrush,
            startAngle = startAngle,
            sweepAngle = totalSweep * fraction,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // ── Tick marks ──
        val tickCount = 30
        val centerX = arcTopLeft.x + arcSize.width / 2f
        val centerY = arcTopLeft.y + arcSize.height / 2f
        val outerRadius = arcSize.width / 2f + strokeWidth * 0.1f
        val innerRadiusMajor = outerRadius + strokeWidth * 0.6f
        val innerRadiusMinor = outerRadius + strokeWidth * 0.35f

        for (i in 0..tickCount) {
            val angle = startAngle + (totalSweep * i / tickCount)
            val radians = angle * (PI / 180f).toFloat()
            val isMajor = i % 5 == 0
            val innerR = if (isMajor) innerRadiusMajor else innerRadiusMinor

            val outerX = centerX + outerRadius * cos(radians)
            val outerY = centerY + outerRadius * sin(radians)
            val innerX = centerX + innerR * cos(radians)
            val innerY = centerY + innerR * sin(radians)

            drawLine(
                color = if (isMajor) SecondaryGray.copy(alpha = 0.5f)
                else SecondaryGray.copy(alpha = 0.2f),
                start = Offset(outerX, outerY),
                end = Offset(innerX, innerY),
                strokeWidth = if (isMajor) thinStroke else thinStroke * 0.6f,
                cap = StrokeCap.Round
            )
        }

        // ── Needle dot at current position ──
        val needleAngle = startAngle + totalSweep * fraction
        val needleRadians = needleAngle * (PI / 180f).toFloat()
        val needleRadius = arcSize.width / 2f
        val needleX = centerX + needleRadius * cos(needleRadians)
        val needleY = centerY + needleRadius * sin(needleRadians)

        // Glow
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = 12.dp.toPx(),
            center = Offset(needleX, needleY)
        )
        // Core dot
        drawCircle(
            color = Color.White,
            radius = 5.dp.toPx(),
            center = Offset(needleX, needleY)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  C. DAILY STEP VIGILANCE CARD
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun DailyStepVigilanceCard(
    steps: Int,
    goal: Int,
    distanceKm: Float,
    activeMinutes: Int
) {
    val fraction = steps.toFloat() / goal.toFloat()
    val percentText = "%.1f".format(fraction * 100)

    // Animate progress circle
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(steps) {
        animatedProgress.animateTo(
            targetValue = fraction,
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header
            CardSectionHeader(
                icon = Icons.Outlined.DirectionsRun,
                title = "DAILY STEP VIGILANCE"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Data content row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Progress circle ──
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 6.dp.toPx()
                        val arcPad = strokeW / 2f
                        val arcSz = Size(
                            size.width - arcPad * 2,
                            size.height - arcPad * 2
                        )

                        // Background track
                        drawArc(
                            color = GaugeTrackGray,
                            startAngle = -225f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = Offset(arcPad, arcPad),
                            size = arcSz,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )

                        // Progress arc
                        drawArc(
                            color = EmeraldMint,
                            startAngle = -225f,
                            sweepAngle = 270f * animatedProgress.value,
                            useCenter = false,
                            topLeft = Offset(arcPad, arcPad),
                            size = arcSz,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }

                    // Center icon
                    Icon(
                        imageVector = Icons.Outlined.DirectionsRun,
                        contentDescription = null,
                        tint = EmeraldMint.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // ── Step count ──
                Column {
                    Text(
                        text = "%,d".format(steps),
                        color = EmeraldMint,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonoFont,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "[$percentText% OF %,d]".format(goal),
                        color = SecondaryGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = MonoFont,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricFooterItem(
                    icon = Icons.Outlined.DirectionsRun,
                    iconColor = ClearBlue,
                    label = "DISTANCE (km):",
                    value = "[$distanceKm]"
                )
                MetricFooterItem(
                    icon = Icons.Outlined.Favorite,
                    iconColor = ClearBlue,
                    label = "ACTIVE MIN (min):",
                    value = "[$activeMinutes]"
                )
            }
        }
    }
}

@Composable
private fun MetricFooterItem(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$label ",
            color = SecondaryGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = ClearBlue,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFont
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  D. CONSISTENCY COMMAND CARD
// ═══════════════════════════════════════════════════════════════════════════

private data class DayChip(
    val dayLabel: String,
    val date: Int,
    val state: ChipState
)

private enum class ChipState { ACTIVE_SELECTED, ACTIVE_STREAK, INACTIVE }

@Composable
private fun ConsistencyCommandCard() {
    val days = listOf(
        DayChip("Mon", 25, ChipState.ACTIVE_SELECTED),
        DayChip("Tue", 26, ChipState.ACTIVE_STREAK),
        DayChip("Wed", 27, ChipState.ACTIVE_STREAK),
        DayChip("Thu", 28, ChipState.ACTIVE_STREAK),
        DayChip("Fri", 29, ChipState.ACTIVE_STREAK),
        DayChip("Sat", 30, ChipState.INACTIVE),
        DayChip("Sun", 31, ChipState.INACTIVE),
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header
            CardSectionHeader(
                icon = Icons.Outlined.LocalFireDepartment,
                title = "CONSISTENCY COMMAND"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Weekly timeline row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                days.forEach { chip ->
                    StreakDayChip(chip)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Streak label
            Text(
                text = "5-Day Metabolic Consistency Streak Active",
                color = SecondaryGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Insight block
            Surface(
                color = InsightBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✦ Maintaining metabolic stability reduces cellular oxidative " +
                                "stress and preserves systemic recovery capacity.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Source: On-Device Gemma (INT8) | Consensus Input: Step + Food + Sleep Logs",
                        color = SecondaryGray.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakDayChip(chip: DayChip) {
    val isSelected = chip.state == ChipState.ACTIVE_SELECTED
    val isActive = chip.state != ChipState.INACTIVE

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Fire icon above
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when (chip.state) {
                        ChipState.ACTIVE_SELECTED -> EmeraldMint
                        ChipState.ACTIVE_STREAK -> ChipActiveBg
                        ChipState.INACTIVE -> ChipInactiveBg
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalFireDepartment,
                contentDescription = null,
                tint = when (chip.state) {
                    ChipState.ACTIVE_SELECTED -> CardSlate
                    ChipState.ACTIVE_STREAK -> EmeraldMint
                    ChipState.INACTIVE -> SecondaryGray.copy(alpha = 0.3f)
                },
                modifier = Modifier.size(16.dp)
            )
        }

        // Day label
        Text(
            text = chip.dayLabel,
            color = if (isActive) Color.White else SecondaryGray.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )

        // Date number
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) EmeraldMint
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${chip.date}",
                color = when {
                    isSelected -> CardSlate
                    isActive -> Color.White
                    else -> SecondaryGray.copy(alpha = 0.5f)
                },
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = MonoFont
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  E. BOTTOM NAVIGATION BAR
// ═══════════════════════════════════════════════════════════════════════════

private data class HubNavItem(val icon: ImageVector, val label: String)

@Composable
private fun HubBottomNavBar() {
    val items = listOf(
        HubNavItem(Icons.Outlined.Home, "Home"),
        HubNavItem(Icons.Outlined.Security, "Threats"),
        HubNavItem(Icons.Outlined.DevicesOther, "Devices"),
        HubNavItem(Icons.Outlined.Settings, "Settings"),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Bar background
        Surface(
            color = NavBarBg,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Top separator
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
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left items
                items.take(2).forEach { item ->
                    HubNavTab(item = item, modifier = Modifier.weight(1f))
                }

                // Center spacer for the floating button
                Spacer(modifier = Modifier.weight(1f))

                // Right items
                items.drop(2).forEach { item ->
                    HubNavTab(item = item, modifier = Modifier.weight(1f))
                }
            }
        }

        // ── Floating center button ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-22).dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2A2A2E),
                            Color(0xFF1A1A1E)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* center action */ }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Command Center",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun HubNavTab(item: HubNavItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = NavInactive,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.label,
            color = NavInactive,
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  FOOTER CAPTION
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun HubFooterCaption() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TECHNICAL ECOSYSTEM: ACTIVITY & RECOVERY HUB",
            color = SecondaryGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "- A Premium, Clinical-Grade Hub visualizing metabolic data and\nbehavioral consistency.",
            color = SecondaryGray.copy(alpha = 0.6f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SHARED UTILITY COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Reusable card section header with icon + title row.
 */
@Composable
private fun CardSectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon in circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ChipActiveBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldMint,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
    }
}
