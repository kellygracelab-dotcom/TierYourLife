package com.artiuillab.tieryourlife.core.theme.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Screens that want the whole window for one task, like the duels: while one
 * is up the rail steps aside. A screen deep inside another says so with
 * [FullScreenTask] rather than navigation having to know what it draws.
 */
@Stable
class FullScreenTasks {
    private var open by mutableIntStateOf(0)

    val any: Boolean get() = open > 0

    internal fun enter() {
        open++
    }

    internal fun leave() {
        open--
    }
}

val LocalFullScreenTasks = staticCompositionLocalOf { FullScreenTasks() }

/** For as long as this is composed, the window belongs to the screen that calls it. */
@Composable
fun FullScreenTask() {
    val tasks = LocalFullScreenTasks.current
    DisposableEffect(tasks) {
        tasks.enter()
        onDispose { tasks.leave() }
    }
}
