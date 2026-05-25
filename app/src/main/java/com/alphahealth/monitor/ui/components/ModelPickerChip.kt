package com.alphahealth.monitor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Represents an AI model that can be selected.
 */
data class AiModelInfo(
    val id: String,
    val displayName: String,
    val sizeLabel: String,
    val isDownloaded: Boolean = false,
    val isInitializing: Boolean = false,
)

/**
 * Pill-shaped chip that shows the currently selected AI model name.
 *
 * Tapping the chip opens a [ModalBottomSheet] with a list of available models.
 * Each model shows its name, size, and download/ready status.
 *
 * Ported from Google AI Edge Gallery's ModelPickerChip pattern.
 *
 * @param currentModel The currently active model
 * @param availableModels All available models (downloaded + downloadable)
 * @param enabled Whether the chip is interactable
 * @param onModelSelected Called when a new model is selected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerChip(
    currentModel: AiModelInfo,
    availableModels: List<AiModelInfo>,
    enabled: Boolean = true,
    onModelSelected: (AiModelInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(enabled = enabled) { showPicker = true }
                    .padding(start = 8.dp, end = 2.dp)
                    .padding(vertical = 4.dp)
                    .graphicsLayer { alpha = if (enabled) 1f else 0.6f },
            ) {
                // Status icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(21.dp),
                ) {
                    Icon(
                        imageVector = if (currentModel.isDownloaded) Icons.Outlined.Check
                        else Icons.Outlined.Memory,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (currentModel.isDownloaded) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Initializing spinner overlay
                    AnimatedVisibility(
                        visible = currentModel.isInitializing,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).alpha(0.5f),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Model name
                Text(
                    text = currentModel.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .widthIn(0.dp, 200.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Dropdown arrow
                Icon(
                    Icons.Rounded.ArrowDropDown,
                    modifier = Modifier.size(20.dp),
                    contentDescription = "Change model",
                )
            }
        }
    }

    // ── Bottom Sheet Model Picker ──
    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Select AI Model",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
                )

                availableModels.forEach { model ->
                    val isSelected = model.id == currentModel.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable {
                                onModelSelected(model)
                                showPicker = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = model.sizeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Status icon
                        Icon(
                            imageVector = if (model.isDownloaded) Icons.Outlined.Check
                            else Icons.Outlined.Download,
                            contentDescription = if (model.isDownloaded) "Ready" else "Download required",
                            tint = if (model.isDownloaded) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
