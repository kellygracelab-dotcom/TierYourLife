package com.artiuillab.tieryourlife.feature.tier.domain.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * How far along the pictures are on a phone that has just been handed
 * somebody's boards.
 *
 * Worth showing only in this direction. Sending happens while the person is
 * using the app and there is nothing for them to wait for; arriving is the
 * case where the board is on screen with holes in it, and a count is the
 * difference between "still coming" and "this is what you get".
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
