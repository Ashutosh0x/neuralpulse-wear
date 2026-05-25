package com.alphahealth.monitor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alphahealth.monitor.ui.theme.emptyStateTitle
import com.alphahealth.monitor.ui.theme.emptyStateContent

/**
 * Centered empty state composable for screens with no data.
 *
 * Displays a prominent icon, title, description, and optional CTA button.
 * Use for:
 * - No watch connected
 * - No health data recorded yet
 * - No AI model downloaded
 *
 * Ported from Google AI Edge Gallery's EmptyState pattern.
 *
 * @param icon The Material icon to display (56dp, onSurfaceVariant tint)
 * @param title The headline text
 * @param description The explanatory body text
 * @param buttonLabel Optional CTA button label
 * @param onButtonClick Optional CTA button click handler
 * @param buttonIcon Optional icon to show inside the button
 */
@Composable
fun HealthEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    buttonLabel: String? = null,
    onButtonClick: (() -> Unit)? = null,
    buttonIcon: ImageVector? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(horizontal = 48.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = title,
            style = emptyStateTitle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Text(
            text = description,
            style = emptyStateContent,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (buttonLabel != null && onButtonClick != null) {
            Box {
                Button(onClick = onButtonClick) {
                    if (buttonIcon != null) {
                        Icon(
                            imageVector = buttonIcon,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(20.dp),
                        )
                    }
                    Text(text = buttonLabel)
                }
            }
        }
    }
}
