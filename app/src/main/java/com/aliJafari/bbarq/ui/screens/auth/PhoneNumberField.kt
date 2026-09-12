package com.aliJafari.bbarq.ui.screens.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.ui.components.PersianText

/**
 * Mobile number input.
 *
 * State always holds raw digits; the grouping into `0912 345 6789` is purely
 * visual, so nothing downstream has to strip separators back out.
 */
@Composable
fun PhoneNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onImeAction: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { PersianText(stringResource(R.string.login_phone)) },
        placeholder = { PersianText(stringResource(R.string.login_phone_hint)) },
        supportingText = {
            PersianText(
                text = stringResource(R.string.login_phone_helper),
                style = MaterialTheme.typography.labelSmall,
            )
        },
        isError = isError,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        textStyle = MaterialTheme.typography.titleMedium,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done,
        ),
        visualTransformation = PhoneVisualTransformation,
    )
}

/** Groups an 11 digit mobile number as 4-3-4. */
private object PhoneVisualTransformation : VisualTransformation {

    private const val FIRST_GAP = 4
    private const val SECOND_GAP = 7

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.take(PHONE_LENGTH)
        val formatted = buildString {
            digits.forEachIndexed { index, char ->
                if (index == FIRST_GAP || index == SECOND_GAP) append(' ')
                append(char)
            }
        }

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val shifted = when {
                    offset <= FIRST_GAP -> offset
                    offset <= SECOND_GAP -> offset + 1
                    else -> offset + 2
                }
                return shifted.coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val shifted = when {
                    offset <= FIRST_GAP -> offset
                    offset <= SECOND_GAP + 1 -> offset - 1
                    else -> offset - 2
                }
                return shifted.coerceIn(0, digits.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
