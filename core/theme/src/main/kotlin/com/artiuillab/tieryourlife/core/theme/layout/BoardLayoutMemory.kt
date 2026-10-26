package com.artiuillab.tieryourlife.core.theme.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How a person left the pool, kept on this device: whether the side column
 * was folded, and how many lines of cards the docked pool shows. Read through
 * a local so the board, several calls below the app, needs no view model of
 * its own for it; the app provides one backed by its preferences.
 */
interface BoardLayoutMemory {
    /** Null until the person folds or opens the side column themselves. */
    var poolColumnCollapsed: Boolean?

    /** Lines of cards in the pool docked under the rows on a tablet held upright: 1, 2 or 4. */
    var poolDockRows: Int

    companion object {
        val DockRowChoices = listOf(1, 2, 4)
        const val DEFAULT_DOCK_ROWS = 2
    }
}

/** For previews, tests and anything drawn outside the app: remembers for as long as it lives. */
class InMemoryBoardLayoutMemory : BoardLayoutMemory {
    override var poolColumnCollapsed: Boolean? by mutableStateOf(null)
    override var poolDockRows: Int by mutableStateOf(BoardLayoutMemory.DEFAULT_DOCK_ROWS)
}

val LocalBoardLayoutMemory = staticCompositionLocalOf<BoardLayoutMemory> { InMemoryBoardLayoutMemory() }
