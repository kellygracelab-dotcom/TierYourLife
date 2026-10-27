package com.artiuillab.tieryourlife.navigation

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.artiuillab.tieryourlife.feature.tier.presentation.cover.CoverHandOffSubject

/**
 * Something the cover asked the main screen to open (§9), and when. It is
 * kept above both screens, so it outlives the activity being made again
 * when the phone opens: opening the phone is what delivers it.
 */
data class HandOff(val subject: CoverHandOffSubject, val startedAtMillis: Long)

/** Where the main screen goes for a hand-off. */
internal sealed interface HandOffDestination {
    data class Board(val listId: Long, val focusTierId: Long?) : HandOffDestination

    data class CommunityList(val publishedId: String, val title: String) : HandOffDestination

    /** The lists, where the app starts anyway. */
    data object Home : HandOffDestination
}

internal object HandOffRules {
    /** How long the cover waits for the phone to open before it gives up and goes back to where it was. */
    const val TIMEOUT_MILLIS = 30_000L

    fun remainingMillis(handOff: HandOff, nowMillis: Long): Long =
        (handOff.startedAtMillis + TIMEOUT_MILLIS - nowMillis).coerceIn(0L, TIMEOUT_MILLIS)

    /** A hand-off the phone opened too late for is dropped, not delivered: the person has moved on. */
    fun stillWanted(handOff: HandOff?, nowMillis: Long): HandOff? = handOff?.takeIf { remainingMillis(it, nowMillis) > 0 }

    fun destinationOf(subject: CoverHandOffSubject): HandOffDestination = when (subject) {
        is CoverHandOffSubject.OwnList -> HandOffDestination.Board(subject.listId, subject.tierId)
        is CoverHandOffSubject.Card -> HandOffDestination.Board(subject.listId, subject.tierId)
        is CoverHandOffSubject.CommunityList -> HandOffDestination.CommunityList(subject.publishedId, subject.title)
        CoverHandOffSubject.App -> HandOffDestination.Home
    }
}

private const val OWN_LIST = "list"
private const val CARD = "card"
private const val COMMUNITY_LIST = "community"
private const val APP = "app"

/** A hand-off as a bundle can hold it: its kind, when it started, then the subject's fields in order. */
internal fun HandOff.toSaved(): List<Any?> = when (val s = subject) {
    is CoverHandOffSubject.OwnList -> listOf(OWN_LIST, startedAtMillis, s.listId, s.title, s.tierId)
    is CoverHandOffSubject.Card -> listOf(CARD, startedAtMillis, s.listId, s.title, s.tierId)
    is CoverHandOffSubject.CommunityList -> listOf(COMMUNITY_LIST, startedAtMillis, s.publishedId, s.title)
    CoverHandOffSubject.App -> listOf(APP, startedAtMillis)
}

/** Null for anything it does not recognise: a hand-off lost is a wait cancelled, not a crash. */
internal fun handOffFromSaved(saved: List<Any?>): HandOff? {
    val started = saved.getOrNull(1) as? Long ?: return null
    val subject = when (saved.firstOrNull()) {
        OWN_LIST -> CoverHandOffSubject.OwnList(
            listId = saved.getOrNull(2) as? Long ?: return null,
            title = saved.getOrNull(3) as? String ?: return null,
            tierId = saved.getOrNull(4) as? Long,
        )
        CARD -> CoverHandOffSubject.Card(
            listId = saved.getOrNull(2) as? Long ?: return null,
            title = saved.getOrNull(3) as? String ?: return null,
            tierId = saved.getOrNull(4) as? Long ?: return null,
        )
        COMMUNITY_LIST -> CoverHandOffSubject.CommunityList(
            publishedId = saved.getOrNull(2) as? String ?: return null,
            title = saved.getOrNull(3) as? String ?: return null,
        )
        APP -> CoverHandOffSubject.App
        else -> return null
    }
    return HandOff(subject, started)
}

internal val HandOffSaver: Saver<HandOff?, Any> = listSaver(
    save = { handOff -> handOff?.toSaved().orEmpty() },
    restore = { saved -> handOffFromSaved(saved) },
)
