package com.aliJafari.bbarq.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.local.AuthStorage
import com.aliJafari.bbarq.data.repository.AuthRepository
import com.aliJafari.bbarq.utils.digitsOnly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val RESEND_COOLDOWN_SECONDS = 60

/**
 * Drives the OTP login.
 *
 * The previous implementation launched `CoroutineScope(Dispatchers.IO)` for each
 * request, so nothing was cancelled when the screen went away, and an
 * IOException from OkHttp took the app down instead of showing an error.
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()
    private val authStorage = AuthStorage(application)

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private var resendJob: Job? = null

    fun onEvent(event: LoginEvent) {
        when (event) {
            LoginEvent.SendOtp -> sendOtp()
            LoginEvent.ResendOtp -> if (_state.value.canResend) sendOtp()
            LoginEvent.VerifyOtp -> verifyOtp()
            LoginEvent.ChangeNumber -> {
                resendJob?.cancel()
                _state.update {
                    it.copy(
                        step = LoginStep.Phone,
                        code = "",
                        error = null,
                        resendSecondsLeft = 0,
                    )
                }
            }

            is LoginEvent.PhoneChanged -> _state.update {
                it.copy(phone = event.value.digitsOnly(PHONE_LENGTH), error = null)
            }

            is LoginEvent.CodeChanged -> _state.update {
                it.copy(code = event.value.digitsOnly(OTP_LENGTH), error = null)
            }
        }
    }

    private fun sendOtp() {
        val phone = _state.value.phone
        if (phone.length != PHONE_LENGTH) {
            _state.update { it.copy(error = string(R.string.login_invalid_phone)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            val response = withContext(Dispatchers.IO) {
                runCatching { repository.sendOtp(phone) }.getOrNull()
            }
            if (response?.status == 200) {
                _state.update {
                    it.copy(isSubmitting = false, step = LoginStep.Code, code = "", error = null)
                }
                startResendCountdown()
            } else {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        error = response?.message ?: string(R.string.network_request_failed),
                    )
                }
            }
        }
    }

    private fun verifyOtp() {
        val current = _state.value
        if (current.code.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            val response = withContext(Dispatchers.IO) {
                runCatching { repository.verifyOtp(current.phone, current.code) }.getOrNull()
            }
            val token = response?.data?.Token
            if (response?.status == 200 && token != null) {
                authStorage.saveToken(token)
                resendJob?.cancel()
                _state.update { it.copy(isSubmitting = false, step = LoginStep.Success, error = null) }
            } else {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        error = response?.message ?: string(R.string.network_request_failed),
                    )
                }
            }
        }
    }

    private fun startResendCountdown() {
        resendJob?.cancel()
        resendJob = viewModelScope.launch {
            var left = RESEND_COOLDOWN_SECONDS
            while (left > 0) {
                _state.update { it.copy(resendSecondsLeft = left) }
                delay(1_000L)
                left--
            }
            _state.update { it.copy(resendSecondsLeft = 0) }
        }
    }

    private fun string(resId: Int): String = getApplication<Application>().getString(resId)
}
