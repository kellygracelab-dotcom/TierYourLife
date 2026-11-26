package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

/**
 * Whether a floating button stays out: gone while the boards scroll on, back
 * when they scroll back or reach either end. On a small phone it otherwise lies
 * on whichever board passes under it; past the last board there is room kept for it.
 */
@Composable
internal fun rememberShownWhileScrollingUp(scroll: LazyGridState): Boolean {
    var shown by remember(scroll) { mutableStateOf(true) }
    LaunchedEffect(scroll) {
        var last = Place(scroll.firstVisibleItemIndex, scroll.firstVisibleItemScrollOffset)
        snapshotFlow { Place(scroll.firstVisibleItemIndex, scroll.firstVisibleItemScrollOffset) to !scroll.canScrollForward }.collect { (now, atEnd) ->
            shown = shownAfter(last, now, shown, atEnd)
            last = now
        }
    }
    return shown
}

internal data class Place(val index: Int, val offset: Int) : Comparable<Place> {
    override fun compareTo(other: Place): Int = compareValuesBy(this, other, Place::index, Place::offset)
}

internal fun shownAfter(last: Place, now: Place, shown: Boolean, atEnd: Boolean = false): Boolean = when {
    now == Place(0, 0) || atEnd -> true
    now > last -> false
    now < last -> true
    else -> shown
}
