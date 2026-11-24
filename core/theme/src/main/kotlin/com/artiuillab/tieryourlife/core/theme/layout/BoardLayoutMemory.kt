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

    /** Lines of cards in the pool under the rows, docked on a tablet or pulled open on a phone: 1, 2 or 4. */
    var poolDockRows: Int

    /** Whether [poolDockRows] was ever pulled to; until then a phone's pool keeps its one line. */
    val poolDockRowsChosen: Boolean

    /** The pool ranked one card at a time: the current card and a button for each tier. */
    var oneCardAtATime: Boolean

    /** Each picture's name on a strip along its foot; on unless turned off, for every list. */
    var namesOnCards: Boolean

    companion object {
        val DockRowChoices = listOf(1, 2, 4)
        const val DEFAULT_DOCK_ROWS = 2
    }
}

/** For previews, tests and anything drawn outside the app: remembers for as long as it lives. */
class InMemoryBoardLayoutMemory : BoardLayoutMemory {
    override var poolColumnCollapsed: Boolean? by mutableStateOf(null)
    private var dockRows by mutableStateOf(BoardLayoutMemory.DEFAULT_DOCK_ROWS)
    override var poolDockRowsChosen: Boolean by mutableStateOf(false)
        private set
    override var poolDockRows: Int
        get() = dockRows
        set(value) {
            dockRows = value
            poolDockRowsChosen = true
        }
    override var oneCardAtATime: Boolean by mutableStateOf(false)
    override var namesOnCards: Boolean by mutableStateOf(true)
}

val LocalBoardLayoutMemory = staticCompositionLocalOf<BoardLayoutMemory> { InMemoryBoardLayoutMemory() }
