package com.aliJafari.bbarq.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Drag-and-drop cannot be driven from a unit test, so the maths behind it is
 * kept pure and pinned down here instead.
 */
class ReorderingTest {

    private val places = listOf("home", "office", "shop", "cabin")

    @Test
    fun `moving down shifts the rows in between up`() {
        assertEquals(listOf("office", "shop", "home", "cabin"), places.moveItem(0, 2))
    }

    @Test
    fun `moving up shifts the rows in between down`() {
        assertEquals(listOf("home", "cabin", "office", "shop"), places.moveItem(3, 1))
    }

    @Test
    fun `moving to the same slot is a no-op and does not copy`() {
        assertSame(places, places.moveItem(2, 2))
    }

    @Test
    fun `moving the last item to the front keeps every element`() {
        assertEquals(listOf("cabin", "home", "office", "shop"), places.moveItem(3, 0))
    }

    @Test
    fun `out of range indices leave the list untouched`() {
        assertSame(places, places.moveItem(-1, 2))
        assertSame(places, places.moveItem(0, 9))
        assertSame(emptyList<String>(), emptyList<String>().moveItem(0, 0))
    }

    @Test
    fun `a drag shorter than half a row stays in its slot`() {
        assertEquals(1, reorderTargetIndex(from = 1, dragOffsetPx = 40f, stridePx = 100f, itemCount = 4))
        assertEquals(1, reorderTargetIndex(from = 1, dragOffsetPx = -40f, stridePx = 100f, itemCount = 4))
    }

    @Test
    fun `a drag past half a row snaps to the next slot`() {
        assertEquals(2, reorderTargetIndex(from = 1, dragOffsetPx = 60f, stridePx = 100f, itemCount = 4))
        assertEquals(0, reorderTargetIndex(from = 1, dragOffsetPx = -60f, stridePx = 100f, itemCount = 4))
    }

    @Test
    fun `multi row drags count every crossed slot`() {
        assertEquals(3, reorderTargetIndex(from = 0, dragOffsetPx = 260f, stridePx = 100f, itemCount = 4))
    }

    @Test
    fun `dragging past either end clamps to the list bounds`() {
        assertEquals(3, reorderTargetIndex(from = 0, dragOffsetPx = 5000f, stridePx = 100f, itemCount = 4))
        assertEquals(0, reorderTargetIndex(from = 3, dragOffsetPx = -5000f, stridePx = 100f, itemCount = 4))
    }

    @Test
    fun `an unmeasured row cannot have moved`() {
        assertEquals(2, reorderTargetIndex(from = 2, dragOffsetPx = 300f, stridePx = 0f, itemCount = 4))
    }

    @Test
    fun `an empty list has no target`() {
        assertEquals(0, reorderTargetIndex(from = 0, dragOffsetPx = 300f, stridePx = 100f, itemCount = 0))
    }
}
