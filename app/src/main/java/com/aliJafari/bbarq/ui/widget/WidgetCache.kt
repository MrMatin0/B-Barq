package com.aliJafari.bbarq.ui.widget

import android.content.Context
import androidx.core.content.edit
import com.aliJafari.bbarq.data.model.Outage
import com.aliJafari.bbarq.data.repository.PlaceOutage
import org.json.JSONArray
import org.json.JSONObject

/** One outage as the widget needs it: the place's identity plus the timings. */
internal data class WidgetOutage(
    val placeName: String,
    val colorKey: String,
    val outage: Outage,
)

internal data class WidgetSnapshot(
    val outages: List<WidgetOutage>,
    val syncedAt: Long,
) {
    val hasEverSynced: Boolean get() = syncedAt > 0L
}

/**
 * The widget's view of the schedule.
 *
 * An app widget is rendered by the launcher whenever it feels like it, so it
 * cannot afford a network round trip and there is nothing in Room for it to read
 * (outages are fetched straight from the API and held in memory). Rather than
 * give the Outage table a cache role it was never designed for -- its primary
 * key is the upstream outage number, which two places on the same feeder can
 * share -- the app writes a small denormalised snapshot here after every
 * successful fetch, and the widget only ever reads.
 */
internal object WidgetCache {

    private const val PREFS = "widget_cache"
    private const val KEY_OUTAGES = "outages"
    private const val KEY_SYNCED_AT = "synced_at"

    private const val FIELD_PLACE = "place"
    private const val FIELD_COLOR = "color"
    private const val FIELD_DATE = "date"
    private const val FIELD_START = "start"
    private const val FIELD_END = "end"

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(context: Context, schedules: List<PlaceOutage>) {
        val array = JSONArray()
        schedules.forEach { schedule ->
            array.put(
                JSONObject().apply {
                    put(FIELD_PLACE, schedule.place.name)
                    put(FIELD_COLOR, schedule.place.colorKey)
                    put(FIELD_DATE, schedule.outage.date.orEmpty())
                    put(FIELD_START, schedule.outage.startTime.orEmpty())
                    put(FIELD_END, schedule.outage.endTime.orEmpty())
                },
            )
        }

        prefs(context).edit {
            putString(KEY_OUTAGES, array.toString())
            putLong(KEY_SYNCED_AT, System.currentTimeMillis())
        }
    }

    fun load(context: Context): WidgetSnapshot {
        val prefs = prefs(context)
        val syncedAt = prefs.getLong(KEY_SYNCED_AT, 0L)
        val raw = prefs.getString(KEY_OUTAGES, null)
        if (raw.isNullOrBlank()) return WidgetSnapshot(emptyList(), syncedAt)

        // A malformed blob must degrade to "nothing cached", never to a crash
        // inside the launcher's process.
        val outages = runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val row = array.getJSONObject(index)
                WidgetOutage(
                    placeName = row.optString(FIELD_PLACE),
                    colorKey = row.optString(FIELD_COLOR),
                    outage = Outage(
                        id = index,
                        reason = null,
                        date = row.optString(FIELD_DATE).takeIf { it.isNotBlank() },
                        time = null,
                        startTime = row.optString(FIELD_START).takeIf { it.isNotBlank() },
                        endTime = row.optString(FIELD_END).takeIf { it.isNotBlank() },
                        billId = null,
                        address = null,
                    ),
                )
            }
        }.getOrDefault(emptyList())

        return WidgetSnapshot(outages, syncedAt)
    }
}
