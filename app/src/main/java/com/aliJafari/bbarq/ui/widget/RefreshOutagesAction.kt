package com.aliJafari.bbarq.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.aliJafari.bbarq.data.repository.OutageRepository
import com.aliJafari.bbarq.data.repository.PlaceOutage
import com.aliJafari.bbarq.data.repository.PlaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The widget's own refresh button.
 *
 * Lets the widget resync without the app being open and without the foreground
 * service running, which was the only way to force a refresh before.
 */
class RefreshOutagesAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        withContext(Dispatchers.IO) {
            val places = PlaceRepository.getInstance(context).getPlaces()
            val outageRepository = OutageRepository(context)

            var anySucceeded = false
            val schedules = places.flatMap { place ->
                val fetched = runCatching { outageRepository.fetchOutages(place.billId) }
                if (fetched.isSuccess) anySucceeded = true
                fetched.getOrDefault(emptyList()).map { PlaceOutage(place, it) }
            }

            // A total failure must not overwrite good data with an empty list:
            // stale numbers on the home screen beat a confident "all clear".
            if (anySucceeded || places.isEmpty()) {
                WidgetCache.save(context, schedules)
            }
        }

        OutageWidget.updateAll(context)
    }
}
