package com.artiuillab.tieryourlife.core.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * When to ask for Play's review card (Danylo 18.11, option B; the moment is Marketing's): the first list
 * finished to the end, one's own or someone else's, from five cards; never in the first ten minutes after
 * installing, once per install, and only where the screen changes, never from a button.
 */
interface ReviewMoments {
    /** A list was just finished: its last card placed, or a game's result closed back to the board. */
    fun listFinished(cards: Int)

    /** Each item is the moment to ask; the activity asks Play. */
    val asks: Flow<Unit>

    object None : ReviewMoments {
        override fun listFinished(cards: Int) = Unit

        override val asks: Flow<Unit> = emptyFlow()
    }

    companion object {
        /** After the last card lands, a moment for the board's own "done" before Play's card. */
        const val FINISHED_PAUSE_MS = 1_000L
    }
}

@Singleton
class OnceReviewMoments internal constructor(
    private val preferences: AppPreferences,
    private val installedAt: () -> Long,
    private val now: () -> Long,
    private val offered: Boolean,
) : ReviewMoments {

    @Inject
    constructor(preferences: AppPreferences, @ApplicationContext context: Context) : this(
        preferences = preferences,
        installedAt = { installTime(context) },
        now = System::currentTimeMillis,
        offered = Features.REVIEW_CARD_OFFERED,
    )

    private val pending = Channel<Unit>(Channel.CONFLATED)

    override val asks: Flow<Unit> = pending.receiveAsFlow()

    override fun listFinished(cards: Int) {
        if (!offered || cards < MIN_CARDS || preferences.reviewAsked()) return
        // Too soon is not "asked": the next finished list after the first ten minutes counts.
        if (now() - installedAt() < FIRST_MINUTES_MS) return
        preferences.markReviewAsked()
        pending.trySend(Unit)
    }

    companion object {
        const val MIN_CARDS = 5
        const val FIRST_MINUTES_MS = 10 * 60 * 1000L

        private fun installTime(context: Context): Long = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        }.getOrDefault(0L)
    }
}
