package com.aliJafari.bbarq.utils

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

private const val PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
private const val ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"

/**
 * The language the UI is currently rendered in.
 *
 * [AppCompatDelegate.getApplicationLocales] is empty until something calls
 * `setApplicationLocales`, which means every caller that runs before
 * `App.onCreate` has applied the stored language (the app widget and the
 * foreground service both can) used to see `null` here. Falling back to the
 * default locale keeps those surfaces consistent with the rest of the app.
 */
private val currentLanguage: String
    get() = AppCompatDelegate.getApplicationLocales().get(0)?.language
        ?: Locale.getDefault().language

/** Renders latin digits as Persian ones, but only while the app locale is fa. */
fun String.toPersianDigitsIfNeeded(): String {
    if (currentLanguage != "fa") return this
    return map { c -> if (c in '0'..'9') PERSIAN_DIGITS[c - '0'] else c }.joinToString("")
}

/**
 * Normalises Persian and Arabic-Indic numerals to latin ones.
 *
 * Soft keyboards on Persian locales emit ۰-۹, which every numeric input in the
 * app has to fold back before it hits the API. This used to be twenty chained
 * replace() calls duplicated across the login screen and the place editor.
 */
fun String.toLatinDigits(): String = map { c ->
    when {
        c in '0'..'9' -> c
        PERSIAN_DIGITS.indexOf(c) >= 0 -> '0' + PERSIAN_DIGITS.indexOf(c)
        ARABIC_DIGITS.indexOf(c) >= 0 -> '0' + ARABIC_DIGITS.indexOf(c)
        else -> c
    }
}.joinToString("")

/** Latin digits only, optionally truncated. Everything else is dropped. */
fun String.digitsOnly(maxLength: Int = Int.MAX_VALUE): String =
    toLatinDigits().filter(Char::isDigit).take(maxLength)
