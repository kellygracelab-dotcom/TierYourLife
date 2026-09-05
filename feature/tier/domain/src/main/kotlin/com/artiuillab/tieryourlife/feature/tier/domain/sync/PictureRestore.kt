package com.artiuillab.tieryourlife.feature.tier.domain.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * How far along the pictures are on a phone just handed somebody's boards.
 * Only this direction is shown: arriving is the case where the board is on
 * screen with holes in it.
 */
interface PictureRestore {

    val restoring: StateFlow<Progress>

    data class Progress(val done: Int, val total: Int) {

        val finished: Boolean get() = total == 0 || done >= total

        companion object {
            val Idle = Progress(done = 0, total = 0)
        }
    }
}
