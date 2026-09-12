package com.aliJafari.bbarq.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aliJafari.bbarq.data.model.Place

@Dao
interface PlaceDao {
    /**
     * `id` only breaks ties: two rows should never share a sortOrder, but a
     * half-applied reorder must still produce a stable list rather than a
     * flickering one.
     */
    @Query("SELECT * FROM places ORDER BY sortOrder ASC, id ASC")
    fun getAll(): List<Place>

    /** Null when there are no places yet. */
    @Query("SELECT MAX(sortOrder) FROM places")
    fun maxSortOrder(): Int?

    @Insert
    fun insert(place: Place): Long

    @Update
    fun update(place: Place)

    @Update
    fun updateAll(places: List<Place>)

    @Delete
    fun delete(place: Place)
}
