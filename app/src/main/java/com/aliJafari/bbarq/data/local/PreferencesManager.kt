package com.aliJafari.bbarq.data.local

import android.content.Context
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun getDarkMode(def: Boolean): Boolean = prefs.getBoolean(KEY_DARK_MODE, def)

    fun setDarkMode(enabled: Boolean) = prefs.edit { putBoolean(KEY_DARK_MODE, enabled) }

    fun getLanguage(): AppLanguage =
        AppLanguage.fromNameOrDefault(prefs.getString(KEY_LANGUAGE, AppLanguage.FA.name))

    fun setLanguage(lang: AppLanguage) = prefs.edit { putString(KEY_LANGUAGE, lang.name) }

    private companion object {
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_LANGUAGE = "language"
    }
}
