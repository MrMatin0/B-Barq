package com.aliJafari.bbarq.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val billId: String,
    val colorKey: String,
    val iconKey: String,
    val reminderOffsetsMask: Int = 0, // bitwise OR of ReminderOffset.bit, 0 = reminders off
    /**
     * Position in the user's own ordering, ascending.
     *
     * Reads used to be ordered by [id], which meant the list was frozen in
     * insertion order. New places are appended by [com.aliJafari.bbarq.data.repository.PlaceRepository],
     * and dragging a card rewrites the whole column so the values stay dense.
     */
    val sortOrder: Int = 0,
) {
    val remindersEnabled: Boolean get() = reminderOffsetsMask != 0
}
