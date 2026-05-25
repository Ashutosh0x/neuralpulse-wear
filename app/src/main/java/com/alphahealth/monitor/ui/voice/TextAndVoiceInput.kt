package com.alphahealth.monitor.ui.voice

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.KeyboardAlt
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dual-mode input component that toggles between text keyboard and voice dictation.
 *
 * Features:
 * - Toggle button switches between keyboard icon and mic icon
 * - Text mode: Rounded pill text field with embedded send button
 * - Voice mode: Placeholder for hold-to-dictate (integrates with speech recognizer)
 * - Animated transitions between modes via AnimatedContent
 * - Disabled state during processing with reduced opacity
 *
 * Ported from Google AI Edge Gallery's TextAndVoiceInput pattern.
 *
 * @param processing Whether the AI is currently processing (disables input)
 * @param onTextSubmit Called when user submits text input
 * @param onVoiceStart Called when user starts voice input
 * @param onVoiceStop Called when user stops voice input
 * @param accentColor Color for the send button background
 */
@Composable
fun TextAndVoiceInput(
    processing: Boolean,
    onTextSubmit: (String) -> Unit,
    onVoiceStart: () -> Unit = {},
    onVoiceStop: () -> Unit = {},
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    clearTextTrigger: Long = 0L,
    defaultTextInputMode: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        var textInputMode by remember { mutableStateOf(defaultTextInputMode) }
        var curTextInput by remember { mutableStateOf("") }

        LaunchedEffect(clearTextTrigger) { curTextInput = "" }

        // Toggle button: switch between text and voice input
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .then(
                    if (!processing) {
                        Modifier.clickable {
                            curTextInput = ""
                            textInputMode = !textInputMode
                            if (!textInputMode) onVoiceStart() else onVoiceStop()
                        }
                    } else {
                        Modifier
                    }
                )
                .graphicsLayer { alpha = if (!processing) 1f else 0.5f }
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                )
                .size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (textInputMode) Icons.Outlined.Mic else Icons.Outlined.KeyboardAlt,
                contentDescription = if (textInputMode) "Switch to voice" else "Switch to keyboard",
                modifier = Modifier.size(24.dp),
            )
        }

        AnimatedContent(
            targetState = textInputMode,
            label = "InputModeSwitch",
        ) { showTextInput ->
            if (showTextInput) {
                // ── Text Input Mode ──
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(28.dp),
                        )
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = curTextInput,
                        enabled = !processing,
                        onValueChange = { curTextInput = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.2.sp,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .padding(start = 16.dp, end = 8.dp)
                            .padding(vertical = 2.dp),
                        minLines = 1,
                        maxLines = 3,
                        decorationBox = { innerTextField ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .padding(vertical = 8.dp)
                                ) {
                                    if (curTextInput.isEmpty()) {
                                        Text(
                                            text = "Ask about your health...",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    innerTextField()
                                }

                                // Send button
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .then(
                                            if (!processing && curTextInput.isNotBlank()) {
                                                Modifier.clickable {
                                                    onTextSubmit(curTextInput)
                                                    curTextInput = ""
                                                }
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .graphicsLayer {
                                            alpha = if (!processing && curTextInput.isNotBlank()) 1f else 0.4f
                                        }
                                        .background(accentColor)
                                        .size(36.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = "Send",
                                        modifier = Modifier.offset(x = 2.dp),
                                        tint = Color.White,
                                    )
                                }
                            }
                        },
                    )
                }
            } else {
                // ── Voice Input Mode ──
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(28.dp),
                        )
                        .heightIn(min = 48.dp)
                        .clickable(enabled = !processing) {
                            // Toggle recording
                            onVoiceStart()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Tap to speak",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
