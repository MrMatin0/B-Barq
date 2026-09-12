package com.aliJafari.bbarq.ui.screens.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aliJafari.bbarq.ui.components.PersianText
import com.aliJafari.bbarq.ui.theme.Spacing

/**
 * Pin-style OTP input.
 *
 * A single invisible text field sits over the boxes, so the platform keeps
 * handling IME, autofill and SMS suggestions while we get full control of the
 * visuals. Each accepted digit fires a light haptic tick and the field
 * auto-submits once it is full.
 */
@Composable
fun OtpCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = OTP_LENGTH,
    isError: Boolean = false,
    autoFocus: Boolean = true,
    onFilled: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(autoFocus) {
        if (autoFocus) runCatching { focusRequester.requestFocus() }
    }
    LaunchedEffect(value.length) {
        if (value.isNotEmpty()) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (value.length == length) onFilled()
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            repeat(length) { index ->
                OtpCell(
                    char = value.getOrNull(index),
                    active = index == value.length.coerceAtMost(length - 1) && value.length < length,
                    isError = isError,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .matchParentSize()
                .focusRequester(focusRequester),
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            singleLine = true,
        )
    }
}

@Composable
private fun OtpCell(
    char: Char?,
    active: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val filled = char != null

    val border by animateColorAsState(
        targetValue = when {
            isError -> scheme.error
            active -> scheme.primary
            filled -> scheme.primary.copy(alpha = 0.45f)
            else -> scheme.outlineVariant
        },
        label = "otpCellBorder",
    )
    val container by animateColorAsState(
        targetValue = if (filled) scheme.surfaceContainerHighest else scheme.surfaceContainerHigh,
        label = "otpCellContainer",
    )
    val scale by animateFloatAsState(if (active) 1.04f else 1f, label = "otpCellScale")

    Box(
        modifier = modifier
            .height(60.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .border(if (active) 2.dp else 1.dp, border, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        if (char != null) {
            PersianText(
                text = char.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(Spacing.sm)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(scheme.outlineVariant.copy(alpha = 0.6f)),
            )
        }
    }
}
