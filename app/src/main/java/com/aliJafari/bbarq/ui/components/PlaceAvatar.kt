package com.aliJafari.bbarq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aliJafari.bbarq.ui.theme.IconSize
import com.aliJafari.bbarq.ui.theme.placeColorOption
import com.aliJafari.bbarq.ui.theme.placeIconOption

/** Circular place identity badge: gradient of the place colour plus its icon. */
@Composable
fun PlaceAvatar(
    colorKey: String,
    iconKey: String,
    modifier: Modifier = Modifier,
    size: Dp = IconSize.avatar,
    ring: Boolean = true,
) {
    val colorOption = placeColorOption(colorKey)
    val iconOption = placeIconOption(iconKey)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        colorOption.color,
                        colorOption.color.copy(alpha = 0.72f),
                    ),
                ),
            )
            .then(
                if (ring) {
                    Modifier.border(1.dp, colorOption.color.copy(alpha = 0.45f), CircleShape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconOption.icon),
            contentDescription = null,
            modifier = Modifier.size(size * 0.5f),
            tint = colorOption.onColor,
        )
    }
}
