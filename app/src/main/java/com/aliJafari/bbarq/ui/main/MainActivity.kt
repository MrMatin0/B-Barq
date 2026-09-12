package com.aliJafari.bbarq.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aliJafari.bbarq.App
import com.aliJafari.bbarq.ForegroundService
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.local.AuthStorage
import com.aliJafari.bbarq.isServiceRunning
import com.aliJafari.bbarq.ui.auth.LoginActivity
import com.aliJafari.bbarq.ui.screens.preferences.PreferencesEffect
import com.aliJafari.bbarq.ui.screens.preferences.PreferencesEvent
import com.aliJafari.bbarq.ui.screens.preferences.PreferencesViewModel
import com.aliJafari.bbarq.ui.screens.schedule.ScheduleEffect
import com.aliJafari.bbarq.ui.screens.schedule.ScheduleViewModel
import com.aliJafari.bbarq.ui.theme.BBarqTheme
import com.aliJafari.bbarq.utils.copyScheduleToClipboard
import com.aliJafari.bbarq.utils.shareSchedule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Platform shell.
 *
 * This file used to be 37KB: twelve mutable state fields, the Room reads, the
 * network fetch loop, reminder scheduling and three screens' worth of
 * composables. Everything but genuine Android-platform work now lives in a
 * ViewModel or a screen package.
 */
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val scheduleViewModel: ScheduleViewModel by viewModels()
    private val preferencesViewModel: PreferencesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (AuthStorage(this).getToken() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        App.applyLanguage(preferencesViewModel.state.value.language)
        observeEffects()
        publishSystemState()

        setContent {
            val mainState by mainViewModel.state.collectAsState()
            val scheduleState by scheduleViewModel.state.collectAsState()
            val preferencesState by preferencesViewModel.state.collectAsState()

            BBarqTheme(darkTheme = preferencesState.darkMode) {
                MainScreen(
                    mainState = mainState,
                    scheduleState = scheduleState,
                    preferencesState = preferencesState,
                    onIntent = mainViewModel::onIntent,
                    onScheduleEvent = scheduleViewModel::onEvent,
                    onPreferencesEvent = preferencesViewModel::onEvent,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        publishSystemState()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) publishSystemState()
    }

    private fun observeEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.effects.collect { effect ->
                        when (effect) {
                            MainEffect.ToggleService -> toggleService()
                            MainEffect.OpenAbout -> openAbout()
                            is MainEffect.RequestPermission -> requestPermission(effect.permission)
                        }
                    }
                }
                launch {
                    scheduleViewModel.effects.collect { effect ->
                        when (effect) {
                            is ScheduleEffect.ShareSchedule ->
                                shareSchedule(this@MainActivity, effect.schedule)

                            is ScheduleEffect.CopySchedule ->
                                copyScheduleToClipboard(this@MainActivity, effect.schedule)

                            ScheduleEffect.OpenPlaceEditor -> {
                                mainViewModel.selectTab(MainTab.Preferences)
                                preferencesViewModel.onEvent(PreferencesEvent.AddPlace)
                            }
                        }
                    }
                }
                launch {
                    preferencesViewModel.effects.collect { effect ->
                        when (effect) {
                            PreferencesEffect.NavigateToLogin -> {
                                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                                finish()
                            }

                            PreferencesEffect.OpenAbout -> openAbout()
                            is PreferencesEffect.ApplyLanguage -> App.applyLanguage(effect.language)
                        }
                    }
                }
            }
        }
    }

    /** Pushes platform facts the ViewModel cannot observe on its own. */
    @SuppressLint("BatteryLife")
    private fun publishSystemState() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val ignoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)

        val missing = buildList {
            if (!hasNotificationPermission()) add(AppPermission.Notifications)
            if (!ignoringBatteryOptimizations) {
                add(AppPermission.BatteryOptimization)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) add(AppPermission.ExactAlarms)
            }
        }

        mainViewModel.onSystemStateChanged(
            serviceRunning = isServiceRunning(this, ForegroundService::class.java),
            missingPermissions = missing,
        )
    }

    private fun requestPermission(permission: AppPermission) {
        when (permission) {
            AppPermission.Notifications -> askNotificationPermission()
            AppPermission.BatteryOptimization -> openBatteryOptimizationSettings()
            AppPermission.ExactAlarms -> openExactAlarmSettings()
        }
    }

    private fun toggleService() {
        val serviceIntent = Intent(this, ForegroundService::class.java)
        if (mainViewModel.state.value.serviceRunning) {
            stopService(serviceIntent)
        } else {
            if (scheduleViewModel.state.value.places.isEmpty()) return
            if (!hasNotificationPermission()) {
                askNotificationPermission()
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
        lifecycleScope.launch {
            delay(SERVICE_STATE_SETTLE_MILLIS)
            publishSystemState()
        }
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun askNotificationPermission() {
        if (hasNotificationPermission()) return
        if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            @SuppressLint("InlinedApi")
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST,
            )
        } else {
            Toast.makeText(
                this,
                getString(R.string.notification_permission_sub),
                Toast.LENGTH_SHORT,
            ).show()
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                },
            )
        }
    }

    @SuppressLint("BatteryLife")
    private fun openBatteryOptimizationSettings() {
        startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:$packageName".toUri()
            },
        )
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", packageName, null)
            },
        )
    }

    private fun openAbout() {
        startActivity(Intent(Intent.ACTION_VIEW, ABOUT_URL.toUri()))
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST = 1002
        const val SERVICE_STATE_SETTLE_MILLIS = 150L
        const val ABOUT_URL = "https://github.com/alijafari-gd/B-Barq"
    }
}
