package com.aliJafari.bbarq

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.aliJafari.bbarq.data.local.AppLanguage
import com.aliJafari.bbarq.data.local.PreferencesManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    lateinit var prefsManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager(this)
        // Previously this hardcoded "fa", so picking English in settings did
        // nothing after a restart.
        applyLanguage(prefsManager.getLanguage())
    }

    companion object {

        /** Applies [language] to the whole process, including RTL mirroring. */
        fun applyLanguage(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.tag),
            )
        }
    }
}
