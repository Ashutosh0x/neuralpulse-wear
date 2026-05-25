package com.alphahealth.monitor.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.alphahealth.monitor.vision.FoodScanResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraEnhance
import androidx.compose.material.icons.outlined.FactCheck

@Composable
fun AiVisionTab(
    scannedFood: FoodScanResult?,
    glycemicRiskPercent: Int,
    onTriggerFoodScan: (String) -> Unit,
    onClearFood: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Viewing Area: Camera Viewport with active bounding boxes drawn directly on local NPU
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = androidx.camera.core.Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview
                                    )
                                } catch (exc: Exception) {
                                    // Handle bindings failure gracefully
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Bounding Box Overlay Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (scannedFood != null) {
                            // Target tracking overlay indicator
                            val strokeWidthPx = 3.dp.toPx()
                            drawRect(
                                color = Color(0xFF10B981),
                                topLeft = Offset(size.width * 0.25f, size.height * 0.25f),
                                size = Size(size.width * 0.5f, size.height * 0.5f),
                                style = Stroke(width = strokeWidthPx)
                            )
                            // Small corner ticks or indicator lines
                            drawCircle(
                                color = Color(0xFF10B981),
                                radius = 6.dp.toPx(),
                                center = Offset(size.width * 0.25f, size.height * 0.25f)
                            )
                        } else {
                            // Calibration scanner indicator lines
                            val strokeWidthPx = 1.5.dp.toPx()
                            drawRect(
                                color = Color.Gray.copy(alpha = 0.5f),
                                topLeft = Offset(size.width * 0.3f, size.height * 0.3f),
                                size = Size(size.width * 0.4f, size.height * 0.4f),
                                style = Stroke(width = strokeWidthPx)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraEnhance,
                            contentDescription = "Camera Permission Required",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please authorize camera access to enable real-time food nutrition scanning.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(text = "Grant Permission", color = Color.White)
                        }
                    }
                }
            }
        }

        // Interaction Area: Ingested Diet History / Slide-up bottom sheet consensus card
        Card(
            shape = RoundedCornerShape(24.dp),
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
                        text = "3-FRAME TEMPORAL CONSENSUS ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Outlined.FactCheck,
                        contentDescription = "Consensus Gate",
                        tint = AlphaMintGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (scannedFood == null) {
                    Text(
                        text = "Point camera at a nutritional item to run local classification (INT8 Quantized MobileNetV3). Choose an item below to simulate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = scannedFood.foodItemName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${scannedFood.baselineCalories} kcal | Confidence: ${String.format("%.0f%%", scannedFood.confidence * 100f)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onClearFood,
                            colors = ButtonDefaults.buttonColors(containerColor = AlphaMintGreen),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(text = "Clear Log", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Macronutrient Distribution",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress indicators for Protein, Carbs, Fats
                    scannedFood.macronutrients.forEach { (macro, value) ->
                        val progressMax = when (macro) {
                            "Protein" -> 50f
                            "Carbs" -> 100f
                            "Fats" -> 50f
                            else -> 100f
                        }
                        val ratio = (value / progressMax).coerceIn(0f, 1f)

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = macro, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    text = "${value}g",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = ratio,
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = when (macro) {
                                    "Protein" -> AlphaMintGreen
                                    "Carbs" -> AlphaWarningAmber
                                    "Fats" -> AlphaAccentBlue
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Glycemic Clearance Curve", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "$glycemicRiskPercent% Glycemic Risk",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (glycemicRiskPercent >= 70) Color(0xFFEF4444) else AlphaMintGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Test Scanner Selectors
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SIMULATED DIET INPUTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onTriggerFoodScan("avocado") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1C1C1E))
                    ) {
                        Text(text = "Avocado", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    OutlinedButton(
                        onClick = { onTriggerFoodScan("chicken") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1C1C1E))
                    ) {
                        Text(text = "Chicken", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    OutlinedButton(
                        onClick = { onTriggerFoodScan("pasta") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1C1C1E))
                    ) {
                        Text(text = "Pasta", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
