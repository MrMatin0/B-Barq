package com.aliJafari.bbarq.domain.model

/**
 * A monitored subscription (one electricity bill id), as the domain sees it.
 *
 * Reminder preferences are a [Set], not a bitmask: the mask is a storage detail
 * and stays in the data layer's mapper.
 */
data class Place(
    val id: Long = 0L,
    val name: String,
    val billId: String,
    val colorKey: String,
    val iconKey: String,
    val reminderOffsets: Set<ReminderOffset> = emptySet(),
) {

    val remindersEnabled: Boolean get() = reminderOffsets.isNotEmpty()

    val hasWellFormedBillId: Boolean get() = billId.length == BILL_ID_LENGTH

    companion object {
        const val BILL_ID_LENGTH = 13
    }
}
