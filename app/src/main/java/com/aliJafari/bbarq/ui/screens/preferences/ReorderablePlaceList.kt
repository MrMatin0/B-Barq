package com.aliJafari.bbarq.ui.screens.preferences

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.model.Place
import com.aliJafari.bbarq.ui.theme.IconSize
import com.aliJafari.bbarq.ui.theme.Spacing
import com.aliJafari.bbarq.utils.reorderTargetIndex

/**
 * Live drag state for the place column.
 *
 * Held in a plain object rather than in loose composable locals because the
 * pointer handler is a long-lived suspending lambda: reading a value captured
 * at composition time would give it a stale row height on the very first drag.
 * Every read here goes through snapshot state, so the gesture always sees
 * current numbers.
 */
private class PlaceDragState {
    var draggingIndex by mutableStateOf<Int?>(null)
    var offsetY by mutableFloatStateOf(0f)
    var rowHeightPx by mutableFloatStateOf(0f)
    var spacingPx by mutableFloatStateOf(0f)
    var itemCount by mutableIntStateOf(0)

    /** One row plus the gap below it. */
    val stride: Float get() = rowHeightPx + spacingPx

    val targetIndex: Int?
        get() = draggingIndex?.let { reorderTargetIndex(it, offsetY, stride, itemCount) }

    fun reset() {
        draggingIndex = null
        offsetY = 0f
    }
}

/**
 * The tracked places, reorderable by dragging a card's handle.
 *
 * Deliberately a plain [Column] rather than a LazyColumn: the caller already
 * hosts this inside one lazy item, and translating rows by hand is far more
 * predictable than fighting lazy layout's own item placement. Places are a
 * handful of rows at most, so nothing is gained by virtualising them.
 *
 * Dragging starts from a dedicated handle after a long press. Both halves of
 * that matter: the handle keeps the whole-card tap working as edit, and the
 * long press keeps the parent list scrollable.
 */
@Composable
fun ReorderablePlaceList(
    places: List<Place>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onEdit: (Place) -> Unit,
    onDelete: (Place) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val spacingPx = with(LocalDensity.current) { Spacing.sm.toPx() }
    val drag = remember { PlaceDragState() }

    // A single place has nowhere to go.
    val reorderable = places.size > 1

    SideEffect {
        drag.spacingPx = spacingPx
        drag.itemCount = places.size
    }

    val draggingIndex = drag.draggingIndex
    val targetIndex = drag.targetIndex

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        places.forEachIndexed { index, place ->
            val dragging = index == draggingIndex

            // Rows between the row's origin and its current slot step aside by
            // exactly one stride, in the opposite direction to the drag.
            val slotShift = when {
                draggingIndex == null || targetIndex == null || dragging -> 0f
                draggingIndex < targetIndex && index in (draggingIndex + 1)..targetIndex ->
                    -drag.stride

                draggingIndex > targetIndex && index in targetIndex until draggingIndex ->
                    drag.stride

                else -> 0f
            }
            val shift by animateFloatAsState(targetValue = slotShift, label = "placeSlotShift")
            val lift by animateFloatAsState(
                targetValue = if (dragging) 1.03f else 1f,
                label = "placeDragLift",
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (dragging) drag.offsetY else shift
                        scaleX = lift
                        scaleY = lift
                        alpha = if (dragging) 0.95f else 1f
                    }
                    .onSizeChanged { size ->
                        if (size.height > 0) drag.rowHeightPx = size.height.toFloat()
                    },
            ) {
                PlaceCard(
                    place = place,
                    onEditClick = { onEdit(place) },
                    onDeleteClick = { onDelete(place) },
                    dragHandle = if (!reorderable) {
                        null
                    } else {
                        {
                            DragHandle(
                                // Keyed on the index too: after a reorder the
                                // same place sits at a different position, and
                                // the handler must not report the old one.
                                modifier = Modifier.pointerInput(place.id, index) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            haptic.performHapticFeedback(
                                                HapticFeedbackType.LongPress,
                                            )
                                            drag.draggingIndex = index
                                            drag.offsetY = 0f
                                        },
                                        onDrag = { _, amount -> drag.offsetY += amount.y },
                                        onDragEnd = {
                                            val from = drag.draggingIndex
                                            val to = drag.targetIndex
                                            drag.reset()
                                            if (from != null && to != null && from != to) {
                                                haptic.performHapticFeedback(
                                                    HapticFeedbackType.TextHandleMove,
                                                )
                                                onMove(from, to)
                                            }
                                        },
                                        onDragCancel = { drag.reset() },
                                    )
                                },
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_drag_handle),
        contentDescription = stringResource(R.string.reorder_place),
        modifier = modifier.size(IconSize.lg),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
