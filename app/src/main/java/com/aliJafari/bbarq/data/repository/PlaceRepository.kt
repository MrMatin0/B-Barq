package com.aliJafari.bbarq.data.repository

import android.content.Context
import androidx.core.content.edit
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.local.ADatabase
import com.aliJafari.bbarq.data.model.Place
import com.aliJafari.bbarq.utils.moveItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Single source of truth for places.
 *
 * Previously every caller built its own instance and re-queried Room, so the
 * schedule tab and the preferences tab could disagree about which places exist
 * until one of them happened to reload. The repository is now a process
 * singleton that publishes [places] as a [StateFlow]; writers update it, and
 * every ViewModel simply observes.
 */
class PlaceRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val dao = ADatabase.getInstance(appContext).PlaceDao()
    private val prefs = appContext.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    /**
     * Blocking read. Kept for [com.aliJafari.bbarq.ForegroundService], which
     * already runs off the main thread and has no coroutine scope of its own.
     */
    fun getPlaces(): List<Place> {
        migrateLegacyBillId()
        return dao.getAll().also { _places.value = it }
    }

    suspend fun refresh(): List<Place> = withContext(Dispatchers.IO) { getPlaces() }

    suspend fun savePlace(place: Place): Place = withContext(Dispatchers.IO) {
        val saved = if (place.id == 0L) {
            // New places go to the bottom of the user's ordering, not to
            // whichever slot a default sortOrder of 0 would collide with.
            val positioned = place.copy(sortOrder = (dao.maxSortOrder() ?: -1) + 1)
            positioned.copy(id = dao.insert(positioned))
        } else {
            dao.update(place)
            place
        }
        _places.value = dao.getAll()
        saved
    }

    suspend fun deletePlace(place: Place) {
        withContext(Dispatchers.IO) {
            dao.delete(place)
            _places.value = dao.getAll()
        }
    }

    /**
     * Moves the place at [fromIndex] to [toIndex] and renumbers the column.
     *
     * The new order is published before the write lands: the drag gesture has
     * already settled the row into its slot on screen, and waiting for Room
     * would snap the list back for a frame. Renumbering everything also
     * densifies the id-derived values left behind by migration 3 -> 4.
     */
    suspend fun movePlace(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return

        val current = _places.value
        if (fromIndex !in current.indices || toIndex !in current.indices) return

        val reordered = current
            .moveItem(fromIndex, toIndex)
            .mapIndexed { index, place -> place.copy(sortOrder = index) }

        _places.value = reordered
        withContext(Dispatchers.IO) {
            dao.updateAll(reordered)
            _places.value = dao.getAll()
        }
    }

    private fun migrateLegacyBillId() {
        if (prefs.getBoolean(LEGACY_BILL_ID_MIGRATED, false)) return

        val existingPlaces = dao.getAll()
        val legacyBillId = prefs.getString("billId", "").orEmpty()
        if (existingPlaces.isEmpty() && legacyBillId.length == BILL_ID_LENGTH) {
            dao.insert(
                Place(
                    name = appContext.getString(R.string.default_place_name),
                    billId = legacyBillId,
                    colorKey = "red",
                    iconKey = "home",
                ),
            )
        }
        prefs.edit(commit = true) { putBoolean(LEGACY_BILL_ID_MIGRATED, true) }
    }

    companion object {
        private const val BILL_ID_LENGTH = 13
        private const val LEGACY_BILL_ID_MIGRATED = "legacy_bill_id_migrated"

        @Volatile
        private var instance: PlaceRepository? = null

        fun getInstance(context: Context): PlaceRepository =
            instance ?: synchronized(this) {
                instance ?: PlaceRepository(context).also { instance = it }
            }
    }
}
