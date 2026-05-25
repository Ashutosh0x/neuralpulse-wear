package com.alphahealth.monitor.ui.chat

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Custom Shape for creating message bubble outlines with configurable corner radii.
 *
 * Creates a rounded rectangle suitable for chat message bubbles.
 * One corner is left sharp (0 radius) to indicate the "tail" direction:
 * - [isUserBubble] = true → hard top-RIGHT corner (message from user)
 * - [isUserBubble] = false → hard top-LEFT corner (message from AI agent)
 *
 * Ported from Google AI Edge Gallery's MessageBubbleShape pattern.
 */
class ChatBubbleShape(
    private val radius: Dp = 20.dp,
    private val isUserBubble: Boolean = false,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radiusPx = with(density) { radius.toPx() }
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    topLeftCornerRadius = if (!isUserBubble) {
                        CornerRadius(0f, 0f)
                    } else {
                        CornerRadius(radiusPx, radiusPx)
                    },
                    topRightCornerRadius = if (isUserBubble) {
                        CornerRadius(0f, 0f)
                    } else {
                        CornerRadius(radiusPx, radiusPx)
                    },
                    bottomLeftCornerRadius = CornerRadius(radiusPx, radiusPx),
                    bottomRightCornerRadius = CornerRadius(radiusPx, radiusPx),
                )
            )
        }
        return Outline.Generic(path)
    }
}
