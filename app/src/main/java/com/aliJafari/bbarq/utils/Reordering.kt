package com.aliJafari.bbarq.utils

import kotlin.math.roundToInt

/**
 * List reordering maths, kept free of Compose and Android on purpose.
 *
 * Drag-and-drop is the one interaction that cannot be exercised from a unit
 * test, so the part that is easy to get wrong (which slot a half-dragged row
 * belongs to, and what the resulting list looks like) lives here instead of
 * inside the gesture handler.
 */

/**
 * A copy of the list with the item at [from] reinserted at [to].
 *
 * Out-of-range indices return the receiver untouched rather than throwing: a
 * gesture can always outlive the list it started on.
 */
fun <T> List<T>.moveItem(from: Int, to: Int): List<T> {
    if (from == to) return this
    if (from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

/**
 * The slot a row dragged by [dragOffsetPx] from position [from] now covers.
 *
 * [stridePx] is one row plus the gap below it. A non-positive stride means the
 * rows have not been measured yet, in which case nothing can have moved.
 */
fun reorderTargetIndex(
    from: Int,
    dragOffsetPx: Float,
    stridePx: Float,
    itemCount: Int,
): Int {
    if (itemCount <= 0) return 0
    val last = itemCount - 1
    if (stridePx <= 0f) return from.coerceIn(0, last)
    return (from + (dragOffsetPx / stridePx).roundToInt()).coerceIn(0, last)
}
