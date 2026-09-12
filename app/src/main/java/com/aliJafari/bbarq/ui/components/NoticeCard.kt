package com.aliJafari.bbarq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.ui.theme.IconSize
import com.aliJafari.bbarq.ui.theme.LocalStatusPalette
import com.aliJafari.bbarq.ui.theme.Spacing

enum class NoticeTone { Error, Warning, Info, Accent }

/**
 * One card for every "you should know this" moment: network failures, missing
 * permissions, available updates. Previously each of those was its own
 * bespoke ElevatedCard with slightly different padding and colours.
 */
@Composable
fun NoticeCard(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    iconRes: Int = R.drawable.baseline_error_24,
    tone: NoticeTone = NoticeTone.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val status = LocalStatusPalette.current
    val container = when (tone) {
        NoticeTone.Error -> scheme.errorContainer
        NoticeTone.Warning -> status.soon.container
        NoticeTone.Info -> scheme.surfaceContainerHigh
        NoticeTone.Accent -> scheme.secondaryContainer
    }
    val content = when (tone) {
        NoticeTone.Error -> scheme.onErrorContainer
        NoticeTone.Warning -> status.soon.content
        NoticeTone.Info -> scheme.onSurface
        NoticeTone.Accent -> scheme.onSecondaryContainer
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .then(if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(IconSize.xl + Spacing.xs)
                .clip(CircleShape)
                .background(content.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(IconSize.md),
                tint = content,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            PersianText(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = content,
                fontWeight = FontWeight.Bold,
            )
            if (!message.isNullOrBlank()) {
                PersianText(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.82f),
                )
            }
        }

        if (actionLabel != null && onAction != null) {
            BBarqButton(
                text = actionLabel,
                onClick = onAction,
                tone = BBarqButtonTone.Ghost,
            )
        }
        if (onDismiss != null) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_close),
                    modifier = Modifier.size(IconSize.md),
                    tint = content.copy(alpha = 0.7f),
                )
            }
        }
    }
}
