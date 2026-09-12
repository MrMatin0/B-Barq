package com.aliJafari.bbarq.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The numeric folding helpers are pure Kotlin, so they are the cheapest place
 * in the codebase to catch a regression: every bill id and phone number the
 * user types on a Persian keyboard goes through them before it reaches the API.
 */
class DigitFormattingTest {

    @Test
    fun `persian numerals fold to latin`() {
        assertEquals("0123456789", "۰۱۲۳۴۵۶۷۸۹".toLatinDigits())
    }

    @Test
    fun `arabic indic numerals fold to latin`() {
        assertEquals("0123456789", "٠١٢٣٤٥٦٧٨٩".toLatinDigits())
    }

    @Test
    fun `folding keeps non digits untouched`() {
        assertEquals("a1-b2", "a۱-b۲".toLatinDigits())
    }

    @Test
    fun `digitsOnly strips separators and spaces`() {
        assertEquals("09123456789", "0912 345-6789".digitsOnly())
    }

    @Test
    fun `digitsOnly truncates to the requested length`() {
        assertEquals("1234567890123", "۱۲۳۴۵۶۷۸۹۰۱۲۳۴۵".digitsOnly(13))
    }

    @Test
    fun `digitsOnly on an empty string stays empty`() {
        assertEquals("", "".digitsOnly(13))
        assertEquals("", "---".digitsOnly(13))
    }
}
