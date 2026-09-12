package com.aliJafari.bbarq.ui.screens.auth

import androidx.compose.runtime.Immutable

/** Iranian mobile numbers are 11 digits including the leading zero. */
const val PHONE_LENGTH = 11

/**
 * Length of the SMS code, and the only place to change it.
 * The provider sends a 6 digit code; keeping this in sync matters because the
 * value both truncates the typed input and sizes the pin field.
 * Verification stays enabled for any non-empty code so a different length from
 * the provider can never lock a user out of the button.
 */
const val OTP_LENGTH = 6

enum class LoginStep { Phone, Code, Success }

@Immutable
data class LoginUiState(
    val step: LoginStep = LoginStep.Phone,
    val phone: String = "",
    val code: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val resendSecondsLeft: Int = 0,
) {
    val canSendOtp: Boolean get() = phone.length == PHONE_LENGTH && !isSubmitting
    val canVerify: Boolean get() = code.isNotEmpty() && !isSubmitting
    val canResend: Boolean get() = resendSecondsLeft == 0 && !isSubmitting
}

sealed interface LoginEvent {
    data object SendOtp : LoginEvent
    data object VerifyOtp : LoginEvent
    data object ResendOtp : LoginEvent
    data object ChangeNumber : LoginEvent
    data class PhoneChanged(val value: String) : LoginEvent
    data class CodeChanged(val value: String) : LoginEvent
}
