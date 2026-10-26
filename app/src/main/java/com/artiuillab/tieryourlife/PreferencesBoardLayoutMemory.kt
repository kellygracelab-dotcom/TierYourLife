package com.artiuillab.tieryourlife

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.theme.layout.BoardLayoutMemory

/** The pool's layout as this device last left it: read once, written through, and observed by the board as state. */
internal class PreferencesBoardLayoutMemory(private val preferences: AppPreferences) : BoardLayoutMemory {
    private var collapsed by mutableStateOf(preferences.poolColumnCollapsed())
    private var dockRows by mutableStateOf(preferences.poolDockRows())

    override var poolColumnCollapsed: Boolean?
        get() = collapsed
        set(value) {
            collapsed = value
            preferences.setPoolColumnCollapsed(value)
        }

    override var poolDockRows: Int
        get() = dockRows
        set(value) {
            dockRows = value
            preferences.setPoolDockRows(value)
        }
}
