package com.aliJafari.bbarq.data.local

/**
 * Languages the app ships translations for.
 *
 * This is persisted state, so it belongs in the data layer. It used to sit at
 * the bottom of a Compose file in `ui.main`, which forced the data layer to
 * import from the UI layer.
 */
enum class AppLanguage(val tag: String, val nativeName: String) {
    FA("fa", "فارسی"),
    EN("en", "EN");

    companion object {
        fun fromNameOrDefault(name: String?): AppLanguage =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: FA
    }
}
