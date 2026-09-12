package com.aliJafari.bbarq

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.aliJafari.bbarq.data.local.AppLanguage
import com.aliJafari.bbarq.data.local.PreferencesManager
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig

class App : Application() {

    lateinit var prefsManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager(this)
        // Previously this hardcoded "fa", so picking English in settings did
        // nothing after a restart.
        applyLanguage(prefsManager.getLanguage())

        AppMetrica.activate(
            this,
            AppMetricaConfig.newConfigBuilder(APP_METRICA_KEY).build(),
        )
    }

    companion object {
        private const val APP_METRICA_KEY = "8e651fd5-277a-45a6-852f-ecd23aefbb92"

        /** Applies [language] to the whole process, including RTL mirroring. */
        fun applyLanguage(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.tag),
            )
        }
    }
}
