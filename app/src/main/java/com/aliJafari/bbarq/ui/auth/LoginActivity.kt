package com.aliJafari.bbarq.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.aliJafari.bbarq.data.local.PreferencesManager
import com.aliJafari.bbarq.ui.screens.auth.LoginScreen
import com.aliJafari.bbarq.ui.screens.auth.LoginViewModel
import com.aliJafari.bbarq.ui.theme.BBarqTheme

/**
 * Hosts the login flow. Nothing but wiring lives here: state belongs to
 * [LoginViewModel] and the UI to `ui/screens/auth`.
 */
class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefsManager = remember { PreferencesManager(applicationContext) }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = remember(systemDark) { prefsManager.getDarkMode(systemDark) }

            BBarqTheme(darkTheme = darkTheme) {
                val state by viewModel.state.collectAsState()
                LoginScreen(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onRestart = ::restartApp,
                )
            }
        }
    }

    /**
     * The session token is read once at process start, so the cleanest way to
     * pick it up is a cold restart. Same behaviour as before, without the bare
     * `System.exit(0)` on the UI thread.
     */
    private fun restartApp() {
        packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        finishAffinity()
        Runtime.getRuntime().exit(0)
    }
}
