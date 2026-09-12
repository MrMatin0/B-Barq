package com.aliJafari.bbarq.domain.error

/**
 * Every failure this app can describe, as a value carried inside
 * [kotlin.Result] rather than thrown across layers.
 *
 * The messages are developer-facing English for logs and crash reports only.
 * User-facing text lives in string resources and is chosen by the UI from the
 * error's *type*: that is what keeps localization out of the data layer.
 *
 * Nothing here should ever carry a token, a phone number or a bill id into a
 * log line.
 */
sealed class DomainError(message: String) : Throwable(message) {

    sealed class Auth(message: String) : DomainError(message) {

        /** The submitted OTP was rejected. */
        data object InvalidCode : Auth("OTP rejected by the server")

        /** The stored bearer token is no longer accepted; re-auth required. */
        data object SessionExpired : Auth("Bearer token rejected with 401")

        /** No token stored at all: the user has never logged in, or logged out. */
        data object NotAuthenticated : Auth("No stored bearer token")

        data object NetworkUnreachable : Auth("Auth request could not reach the server")

        data class Server(val statusCode: Int?, val detail: String?) :
            Auth("Auth request failed, status=$statusCode detail=$detail")
    }

    sealed class Outages(message: String) : DomainError(message) {

        data class InvalidBillIdLength(val actualLength: Int) :
            Outages("Bill id must be 13 digits, got $actualLength")

        data object UnknownBillId : Outages("Server does not recognise this bill id")

        data class Parse(val detail: String) :
            Outages("Could not read the outage payload: $detail")

        data object NetworkUnreachable : Outages("Outage request could not reach the server")

        data class Server(val statusCode: Int?, val detail: String?) :
            Outages("Outage request failed, status=$statusCode detail=$detail")
    }

    sealed class Storage(message: String) : DomainError(message) {

        data class Read(val detail: String) : Storage("Read failed: $detail")

        data class Write(val detail: String) : Storage("Write failed: $detail")
    }

    sealed class Reminder(message: String) : DomainError(message) {

        data object ExactAlarmsNotPermitted : Reminder("SCHEDULE_EXACT_ALARM is not granted")

        data object TriggerInThePast : Reminder("Reminder trigger time has already passed")

        data class Failed(val detail: String) : Reminder("Could not schedule reminder: $detail")
    }
}
