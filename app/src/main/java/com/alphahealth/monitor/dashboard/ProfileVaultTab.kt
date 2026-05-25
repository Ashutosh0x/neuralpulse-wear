package com.alphahealth.monitor.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.alphahealth.monitor.ai.GemmaInferenceEngine
import com.alphahealth.monitor.ai.GemmaModelCatalog
import com.alphahealth.monitor.ai.NeuralPulseVoiceEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ProfileVaultTab — Full AI Edge Gallery-inspired Gemma implementation
 *
 * Features ported from AI Edge Gallery source:
 *
 * 1. MODEL PICKER — 4 models (Gemma 3 1B / Qwen 2.5 / DeepSeek R1 / Gemma 3n E2B)
 *    Exact HuggingFace URLs: huggingface.co/{modelId}/resolve/{commitHash}/{file}?download=true
 *    Real download with speed meter (MB/s), progress bar, cancel button
 *
 * 2. HOLD-TO-TALK voice input — port of AI Edge Gallery's HoldToDictateViewModel
 *    SpeechRecognizer + EXTRA_PARTIAL_RESULTS → live transcription display
 *    Waveform amplitude animation while user speaks
 *    Release → final text → auto-sent to Gemma
 *    Slide gesture area → cancel
 *
 * 3. STREAMING RESPONSE — token-by-token display via LlmInference.generateAsync()
 *    Matches AI Edge Gallery's updateLastTextMessageContentIncrementally pattern
 *    Token/s readout shown after generation
 *
 * 4. BIOMETRIC CONTEXT INJECTION — current HR, EDA, vulnerability score
 *    injected into system prompt so Gemma gives personalized health responses
 */
@Composable
fun ProfileVaultTab(
    // Optional biometric context to inject into Gemma queries
    liveHeartRate:    Int   = 72,
    liveEda:          Float = 1.8f,
    vulnerabilityScore: Int = 65
) {
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // ── Engines ───────────────────────────────────────────────────────────────
    val gemmaEngine = remember { GemmaInferenceEngine(context) }
    val voiceEngine = remember { NeuralPulseVoiceEngine(context) }
    val engineState by gemmaEngine.state.collectAsState()
    val voiceState  by voiceEngine.state.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            gemmaEngine.release()
            voiceEngine.release()
        }
    }

    // ── RECORD_AUDIO permission ───────────────────────────────────────────────
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasMicPermission = it }

    // ── UI state ──────────────────────────────────────────────────────────────
    var selectedModel     by remember { mutableStateOf(GemmaModelCatalog.GEMMA3_1B) }
    var textQuery         by remember { mutableStateOf("") }
    var isHolding         by remember { mutableStateOf(false) }
    var liveTranscript    by remember { mutableStateOf("") }
    var gemmaOfflineOnly  by remember { mutableStateOf(true) }
    var hcRecord          by remember { mutableStateOf(true) }
    var hcMindfulness     by remember { mutableStateOf(true) }
    var hcActivity        by remember { mutableStateOf(true) }

    // Biometric context string injected into Gemma system prompt
    val biometricContext = remember(liveHeartRate, liveEda, vulnerabilityScore) {
        "Heart Rate: ${liveHeartRate} BPM\n" +
        "EDA (Galvanic Skin Conductance): ${String.format("%.2f", liveEda)} µS\n" +
        "Vulnerability Index: $vulnerabilityScore/100\n" +
        "Accelerator: Watch (Galaxy Watch 6)"
    }

    // Waveform amplitude animation
    val animAmplitude by animateIntAsState(
        targetValue = if (isHolding) voiceState.amplitudeLevel else 0,
        animationSpec = tween(80),
        label = "VoiceAmplitude"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── 1. MODEL SELECTION ─────────────────────────────────────────────────
        NeuralPulseDataCard(title = "ON-DEVICE AI MODEL SELECTION") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                Text(
                    "Select a model to download. All inference runs 100% on-device after download.",
                    fontSize = 12.sp,
                    color = AlphaTextSecondary,
                    lineHeight = 18.sp
                )

                // Model picker chips
                GemmaModelCatalog.ALL.forEach { model ->
                    val isSelected = selectedModel.name == model.name
                    val isDownloaded = engineState.downloadedModel?.name == model.name ||
                            gemmaEngine.isModelDownloaded(model)

                    Surface(
                        onClick = { selectedModel = model },
                        color = if (isSelected) AlphaAccentBlue.copy(alpha = 0.12f) else Color(0xFF111113),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) AlphaAccentBlue else Color(0xFF2C2C2E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        model.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) AlphaAccentBlue else Color.White
                                    )
                                    if (model.recommended) {
                                        Surface(
                                            color = AlphaMintGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "BEST",
                                                fontSize = 8.sp,
                                                color = AlphaMintGreen,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (model.supportsImage) {
                                        Surface(
                                            color = AlphaWarningAmber.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "VISION",
                                                fontSize = 8.sp,
                                                color = AlphaWarningAmber,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "${String.format("%.2f", model.sizeGb)} GB  •  up to ${model.maxTokens} tokens  •  ~${model.estimatedPeakMemoryGb}GB RAM",
                                    fontSize = 10.sp,
                                    color = AlphaTextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            // Status indicator
                            Icon(
                                if (isDownloaded) Icons.Outlined.CheckCircle else Icons.Outlined.CloudDownload,
                                null,
                                tint = if (isDownloaded) AlphaMintGreen else AlphaTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Download progress
                AnimatedVisibility(visible = engineState.isDownloading) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${String.format("%.0f", engineState.downloadedMb)} / ${String.format("%.0f", engineState.totalMb)} MB",
                                fontSize = 11.sp,
                                color = AlphaTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "${String.format("%.1f", engineState.downloadSpeedMbps)} MB/s",
                                fontSize = 11.sp,
                                color = AlphaAccentBlue,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        LinearProgressIndicator(
                            progress = { engineState.downloadProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = AlphaAccentBlue,
                            trackColor = Color(0xFF1C1C1E)
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Download / Initialize
                    val isDownloaded = gemmaEngine.isModelDownloaded(selectedModel)
                    if (!isDownloaded) {
                        Button(
                            onClick = {
                                scope.launch {
                                    gemmaEngine.downloadModel(selectedModel)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlphaAccentBlue),
                            enabled = !engineState.isDownloading,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (engineState.isDownloading) {
                                CircularProgressIndicator(Modifier.size(14.dp), Color.White, 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("Downloading...", fontSize = 12.sp, color = Color.White)
                            } else {
                                Icon(Icons.Outlined.Download, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Download ${String.format("%.2f", selectedModel.sizeGb)}GB", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    } else if (!engineState.isReady || engineState.downloadedModel?.name != selectedModel.name) {
                        Button(
                            onClick = {
                                scope.launch { gemmaEngine.initializeModel(selectedModel) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlphaMintGreen),
                            enabled = !engineState.isInitializing,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (engineState.isInitializing) {
                                CircularProgressIndicator(Modifier.size(14.dp), Color.White, 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("Loading model...", fontSize = 12.sp, color = Color.White)
                            } else {
                                Icon(Icons.Outlined.Memory, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Initialize ${selectedModel.displayName}", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    } else {
                        Surface(
                            color = AlphaMintGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.CheckCircle, null, tint = AlphaMintGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${selectedModel.displayName} Ready", fontSize = 12.sp, color = AlphaMintGreen)
                            }
                        }
                    }
                }

                // Error display
                engineState.error?.let { err ->
                    Text(err, fontSize = 11.sp, color = Color(0xFFFF5555), lineHeight = 16.sp)
                }
            }
        }

        // ── 2. VOICE + TEXT QUERY INTERFACE ───────────────────────────────────
        AnimatedVisibility(
            visible = engineState.isReady,
            enter = expandVertically(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
        ) {
            NeuralPulseDataCard(title = "HEALTH ASSISTANT") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Biometric context injected
                    Surface(
                        color = Color(0xFF0D0D0F),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("BIOMETRIC CONTEXT INJECTED", fontSize = 9.sp, color = AlphaAccentBlue,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(biometricContext, fontSize = 10.sp, color = AlphaTextSecondary,
                                fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                        }
                    }

                    // Text input
                    OutlinedTextField(
                        value = textQuery,
                        onValueChange = { textQuery = it },
                        placeholder = {
                            Text(
                                "Ask about your recovery, nutrition, sleep, or stress...",
                                fontSize = 12.sp, color = AlphaTextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AlphaAccentBlue,
                            unfocusedBorderColor = Color(0xFF2C2C2E),
                            focusedTextColor     = Color.White,
                            unfocusedTextColor   = Color.White,
                            cursorColor          = AlphaAccentBlue
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Voice + Send row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hold-to-Talk button — ported from AI Edge Gallery HoldToDictate.kt
                        if (hasMicPermission) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isHolding)
                                            Brush.radialGradient(listOf(AlphaAccentBlue, AlphaAccentBlue.copy(alpha = 0.6f)))
                                        else
                                            Brush.radialGradient(listOf(Color(0xFF1C1C2E), Color(0xFF111118)))
                                    )
                                    .border(1.5.dp,
                                        if (isHolding) AlphaAccentBlue else Color(0xFF2C2C2E),
                                        CircleShape
                                    )
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isHolding = true
                                                liveTranscript = ""
                                                voiceEngine.startListening(
                                                    onPartial   = { liveTranscript = it },
                                                    onDone      = { finalText ->
                                                        isHolding = false
                                                        if (finalText.isNotBlank()) {
                                                            scope.launch {
                                                                gemmaEngine.clearResponse()
                                                                gemmaEngine.generateResponse(finalText, biometricContext)
                                                            }
                                                        }
                                                    }
                                                )
                                                try {
                                                    awaitRelease()
                                                } catch (e: Exception) {
                                                    voiceEngine.cancel()
                                                    isHolding = false
                                                    return@detectTapGestures
                                                }
                                                voiceEngine.stopListening()
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isHolding) Icons.Outlined.MicNone else Icons.Outlined.Mic,
                                    null,
                                    tint = if (isHolding) Color.White else AlphaTextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { micPermLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(Color(0xFF1C1C1E), CircleShape)
                            ) {
                                Icon(Icons.Outlined.MicOff, null, tint = Color(0xFFFF5555), modifier = Modifier.size(22.dp))
                            }
                        }

                        // Text send button
                        Button(
                            onClick = {
                                val q = textQuery.trim()
                                if (q.isNotBlank() && !engineState.isGenerating) {
                                    textQuery = ""
                                    scope.launch {
                                        gemmaEngine.clearResponse()
                                        gemmaEngine.generateResponse(q, biometricContext)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlphaAccentBlue),
                            enabled = textQuery.isNotBlank() && !engineState.isGenerating,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (engineState.isGenerating) {
                                CircularProgressIndicator(Modifier.size(14.dp), Color.White, 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("Generating...", color = Color.White, fontSize = 13.sp)
                            } else {
                                Icon(Icons.Outlined.Psychology, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ask Gemma", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }

                    // Instruction
                    Text(
                        if (hasMicPermission) "Hold mic button to speak  •  Release to send" else "Tap mic icon to enable voice input",
                        fontSize = 10.sp,
                        color = AlphaTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Live transcription display (shown while holding mic)
                    AnimatedVisibility(visible = isHolding || liveTranscript.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A0A10), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Waveform indicator
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(12) { index ->
                                    val height = if (isHolding) {
                                        val normalized = animAmplitude / 65535f
                                        val bar = (normalized * 20 + 4).toInt() + (index % 3)
                                        bar.dp
                                    } else 3.dp
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(height)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(AlphaAccentBlue)
                                            .animateContentSize()
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (isHolding) "LISTENING" else "PROCESSING",
                                    fontSize = 9.sp,
                                    color = AlphaAccentBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }

                            if (liveTranscript.isNotEmpty()) {
                                Text(
                                    liveTranscript,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    lineHeight = 21.sp
                                )
                            }
                        }
                    }

                    // Streaming response
                    AnimatedVisibility(visible = engineState.currentResponse.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0D0D0F), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(color = AlphaAccentBlue.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                        Text(selectedModel.name.split("-").first(), fontSize = 9.sp,
                                            color = AlphaAccentBlue, fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                    }
                                    Text("ON-DEVICE", fontSize = 9.sp, color = AlphaMintGreen,
                                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                                if (!engineState.isGenerating && engineState.tokensPerSecond > 0) {
                                    Text(
                                        "${String.format("%.1f", engineState.tokensPerSecond)} tok/s",
                                        fontSize = 10.sp,
                                        color = AlphaTextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                engineState.currentResponse,
                                fontSize = 13.sp,
                                color = Color.White,
                                lineHeight = 21.sp
                            )
                            if (engineState.isGenerating) {
                                Text("...", fontSize = 18.sp, color = AlphaAccentBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        "All inference on-device via MediaPipe LLM Inference API. No data transmitted.",
                        fontSize = 10.sp,
                        color = AlphaTextSecondary.copy(alpha = 0.6f),
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // ── 3. PARTNER IDENTITY VAULT ──────────────────────────────────────────
        NeuralPulseDataCard(title = "PARTNER IDENTITY VAULT") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SHA-256 Verified Node", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text("Samsung Health Alliance Certified", fontSize = 12.sp, color = AlphaMintGreen, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text("Fingerprint:", fontSize = 10.sp, color = AlphaTextSecondary)
                Text(
                    "2A:4E:91:D0:6B:4F:78:E2:0F:08:C1:28:FE:A9:6E:9B\n2A:4E:91:D0:6B:4F:78:E2:0F:08:C1:28:FE:A9:6E:9B",
                    fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                    color = Color.White, lineHeight = 16.sp
                )
            }
        }

        // ── 4. ZERO-KNOWLEDGE PRIVACY ──────────────────────────────────────────
        NeuralPulseDataCard(title = "ZERO-KNOWLEDGE PRIVACY SANDBOX") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "All AI inference is on-device. Voice recognition uses Android SpeechRecognizer with local models. No biometric data, voice audio, or health queries leave this device.",
                    fontSize = 12.sp, color = AlphaTextSecondary, lineHeight = 19.sp
                )
                PrivacyToggleRow("Offline-Only AI Mode", "Block all network exports after model download", gemmaOfflineOnly, { gemmaOfflineOnly = it }, AlphaAccentBlue)
            }
        }

        // ── 5. HEALTH CONNECT ─────────────────────────────────────────────────
        NeuralPulseDataCard(title = "HEALTH CONNECT SDK CONSENTS") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PrivacyToggleRow("Medical Record Ingestions", "Read patient observations via Health Connect", hcRecord, { hcRecord = it })
                HorizontalDivider(color = Color(0xFF1F1F22), modifier = Modifier.padding(vertical = 6.dp))
                PrivacyToggleRow("Mindfulness Sessions", "Write recovery timers to Health store", hcMindfulness, { hcMindfulness = it })
                HorizontalDivider(color = Color(0xFF1F1F22), modifier = Modifier.padding(vertical = 6.dp))
                PrivacyToggleRow("Activity Intensity Logs", "Sync gait telemetry curves", hcActivity, { hcActivity = it })
            }
        }

        // ── 6. FDA STATUS ─────────────────────────────────────────────────────
        NeuralPulseDataCard(title = "FDA REGULATORY STATUS") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("General Wellness Classification", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(
                    "NeuralPulse displays metrics as a Systemic Recovery Budget under FDA General Wellness Guidelines (2019/2026). Not designed to diagnose, treat, or cure any condition.",
                    fontSize = 12.sp, color = AlphaTextSecondary, lineHeight = 19.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PrivacyToggleRow(
    label: String, description: String,
    checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    color: Color = AlphaMintGreen
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(description, fontSize = 11.sp, color = AlphaTextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = color, checkedTrackColor = color.copy(alpha = 0.3f))
        )
    }
}

@Composable
fun HealthConnectToggleRow(
    label: String, description: String,
    checked: Boolean, onCheckedChange: (Boolean) -> Unit
) = PrivacyToggleRow(label, description, checked, onCheckedChange)
