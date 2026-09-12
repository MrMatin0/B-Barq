package com.aliJafari.bbarq.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.local.AuthStorage
import com.aliJafari.bbarq.data.repository.PlaceOutage
import com.aliJafari.bbarq.data.repository.PlaceRepository
import com.aliJafari.bbarq.ui.theme.placeColorOption
import com.aliJafari.bbarq.utils.ScheduleUrgency
import com.aliJafari.bbarq.utils.relativeStatus
import com.aliJafari.bbarq.utils.startEpochMillis
import com.aliJafari.bbarq.utils.toPersianDigitsIfNeeded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** How many places fit before the widget starts summarising. */
private const val MAX_VISIBLE_ROWS = 4

private data class WidgetRow(
    val placeName: String,
    val accent: Color,
    val headline: String,
    val timeRange: String,
    val urgent: Boolean,
)

/**
 * Everything the widget draws, resolved before composition.
 *
 * Strings and colours are looked up on the suspending side rather than inside
 * the Glance tree: the widget is rendered into RemoteViews for another process,
 * and keeping the composition a pure function of plain data makes it far easier
 * to reason about.
 */
private data class WidgetViewState(
    val title: String,
    val syncedLabel: String,
    val refreshLabel: String,
    val message: String?,
    val rows: List<WidgetRow>,
    val moreLabel: String?,
)

object OutageWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = buildViewState(context)
        provideContent { WidgetBody(state) }
    }
}

class OutageWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OutageWidget
}

/**
 * Writes the snapshot the widget reads and asks every placed instance to
 * repaint. Safe to call when no widget is on a home screen.
 */
suspend fun publishOutagesToWidget(context: Context, schedules: List<PlaceOutage>) {
    WidgetCache.save(context, schedules)
    runCatching { OutageWidget.updateAll(context) }
}

private suspend fun buildViewState(context: Context): WidgetViewState {
    val snapshot = WidgetCache.load(context)
    val signedIn = AuthStorage(context).getToken() != null
    val places = withContext(Dispatchers.IO) {
        PlaceRepository.getInstance(context).getPlaces()
    }

    // Same rule as the persistent notification: finished outages are noise.
    val active = snapshot.outages
        .map { it to relativeStatus(it.outage, context) }
        .filter { (_, status) -> status.urgency != ScheduleUrgency.ENDED }
        .sortedBy { (entry, _) -> entry.outage.startEpochMillis() }

    val unknown = context.getString(R.string.value_not_available)
    val rows = active.take(MAX_VISIBLE_ROWS).map { (entry, status) ->
        WidgetRow(
            placeName = entry.placeName,
            accent = placeColorOption(entry.colorKey).color,
            headline = status.label.toPersianDigitsIfNeeded(),
            timeRange = context.getString(
                R.string.schedule_time_range,
                entry.outage.startTime?.takeIf { it.isNotBlank() } ?: unknown,
                entry.outage.endTime?.takeIf { it.isNotBlank() } ?: unknown,
            ).toPersianDigitsIfNeeded(),
            urgent = status.urgency == ScheduleUrgency.ONGOING ||
                status.urgency == ScheduleUrgency.SOON,
        )
    }
    val hiddenCount = (active.size - rows.size).coerceAtLeast(0)

    return WidgetViewState(
        title = context.getString(R.string.widget_label),
        syncedLabel = if (snapshot.hasEverSynced) {
            context.getString(R.string.widget_synced_at, formatClock(snapshot.syncedAt))
                .toPersianDigitsIfNeeded()
        } else {
            context.getString(R.string.widget_never_synced)
        },
        refreshLabel = context.getString(R.string.refresh),
        message = when {
            !signedIn -> context.getString(R.string.widget_signed_out)
            places.isEmpty() -> context.getString(R.string.widget_no_places)
            !snapshot.hasEverSynced -> context.getString(R.string.widget_never_synced)
            rows.isEmpty() -> context.getString(R.string.widget_all_clear)
            else -> null
        },
        rows = rows,
        moreLabel = hiddenCount
            .takeIf { it > 0 }
            ?.let { context.getString(R.string.widget_more_places, it).toPersianDigitsIfNeeded() },
    )
}

/** Latin digits on purpose: [toPersianDigitsIfNeeded] folds them afterwards. */
private fun formatClock(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))

@Composable
private fun WidgetBody(state: WidgetViewState) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(20.dp)
                .padding(14.dp),
        ) {
            WidgetHeader(
                title = state.title,
                syncedLabel = state.syncedLabel,
                refreshLabel = state.refreshLabel,
            )
            Spacer(GlanceModifier.height(10.dp))

            if (state.message != null) {
                Text(
                    text = state.message,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 13.sp,
                    ),
                    maxLines = 3,
                )
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(state.rows) { row -> OutageRow(row) }
                    state.moreLabel?.let { label ->
                        item {
                            Text(
                                text = label,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 11.sp,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader(title: String, syncedLabel: String, refreshLabel: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Text(
                text = syncedLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp,
                ),
                maxLines = 1,
            )
        }
        Image(
            provider = ImageProvider(R.drawable.ic_renew),
            contentDescription = refreshLabel,
            modifier = GlanceModifier
                .size(22.dp)
                .clickable(actionRunCallback<RefreshOutagesAction>()),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

@Composable
private fun OutageRow(row: WidgetRow) {
    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The place's identity colour, same swatch as in the app.
            Spacer(
                GlanceModifier
                    .size(8.dp)
                    .cornerRadius(4.dp)
                    .background(ColorProvider(row.accent)),
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = row.placeName,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            Text(
                text = row.headline,
                style = TextStyle(
                    color = if (row.urgent) {
                        GlanceTheme.colors.error
                    } else {
                        GlanceTheme.colors.primary
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
        Text(
            text = row.timeRange,
            modifier = GlanceModifier.padding(start = 14.dp),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp,
            ),
            maxLines = 1,
        )
    }
}
