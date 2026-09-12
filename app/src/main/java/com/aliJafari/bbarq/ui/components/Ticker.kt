package com.aliJafari.bbarq.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * A wall clock that recomposes its readers on an interval.
 *
 * Relative labels ("in 2 hours") used to be computed once and then go stale
 * until the whole list was refetched. Hoisting a single ticker keeps every
 * countdown honest with exactly one coroutine for the whole screen.
 */
@Composable
fun rememberTickingNow(intervalMillis: Long = 30_000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(intervalMillis) {
        while (true) {
            delay(intervalMillis)
            now = System.currentTimeMillis()
        }
    }
    return now
}
