package com.aliJafari.bbarq.domain.model

/** An outage together with the place it belongs to, for notifications and reminders. */
data class PlaceOutage(
    val place: Place,
    val outage: Outage,
)
