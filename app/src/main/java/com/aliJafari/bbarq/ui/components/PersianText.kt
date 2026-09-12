package com.aliJafari.bbarq.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.aliJafari.bbarq.utils.toPersianDigitsIfNeeded

/**
 * Text that always localises its digits.
 *
 * The old code called `toPersianDigitsIfNeeded()` at ~15 call sites and missed
 * several of them, so Persian screens showed mixed ۱۲۳ / 123 numerals. Using
 * this instead of [Text] makes correct behaviour the default.
 */
@Composable
fun PersianText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign? = null,
    softWrap: Boolean = true,
) {
    Text(
        text = text.toPersianDigitsIfNeeded(),
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = maxLines,
        minLines = minLines,
        overflow = overflow,
        textAlign = textAlign,
        softWrap = softWrap,
    )
}
