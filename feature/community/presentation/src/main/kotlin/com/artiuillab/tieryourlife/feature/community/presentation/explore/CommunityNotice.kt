package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.annotation.StringRes
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.SectionCatalog
import com.artiuillab.tieryourlife.feature.community.presentation.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/** What a Community screen says in its snackbar. */
sealed interface CommunityNotice {
    data class RefreshFailed(val showingFromMillis: Long) : CommunityNotice
    data object FollowFailed : CommunityNotice

    /** [sectionId] is named on screen, where the words are. */
    data class NowFollowing(val sectionId: String) : CommunityNotice

    /** With Undo, which follows the section again. */
    data class Unfollowed(val sectionId: String) : CommunityNotice
    data object ActionFailed : CommunityNotice

    /** A proposal withdrawn too late: a moderator had approved, declined or merged it. [name] is the one it went out with. */
    data class AlreadyDecided(val name: String, val status: SectionStatus) : CommunityNotice {
        @get:StringRes
        val words: Int get() = when (status) {
            SectionStatus.Approved -> R.string.section_withdraw_already_approved
            SectionStatus.Merged -> R.string.section_withdraw_already_merged
            SectionStatus.Pending, SectionStatus.Declined -> R.string.section_withdraw_already_declined
        }
    }
}

/** Withdraws a proposal or puts away its notice, and says so when it did not go or a moderator had decided it first. */
internal suspend fun SectionCatalog.withdrawTelling(sectionId: String, tell: suspend (CommunityNotice) -> Unit) {
    val name = directory.value.find(sectionId)?.name.orEmpty()
    withdraw(sectionId).fold(
        onSuccess = { decided -> if (decided != null) tell(CommunityNotice.AlreadyDecided(name, decided)) },
        onFailure = { tell(CommunityNotice.ActionFailed) },
    )
}

/**
 * Hiding and reporting from a feed's long-press menu, the same on every feed:
 * local and silent, with a note where the card was, and a report put back on
 * screen if it could not be sent.
 */
internal class ListMenuActions(
    private val preferences: AppPreferences,
    private val community: CommunityRepository,
    private val scope: CoroutineScope,
    private val failed: suspend () -> Unit,
) {
    fun isVisible(summary: PublishedListSummary): Boolean =
        summary.id !in preferences.hiddenListIds() && summary.authorUid !in preferences.hiddenAuthorUids()

    fun hideList(loader: FeedLoader, summary: PublishedListSummary) {
        preferences.hideList(summary.id, summary.title)
        loader.refilter(loader.pane.value.justHidden + (summary.id to false))
    }

    fun hideAuthor(loaders: List<FeedLoader>, authorUid: String, name: String) {
        preferences.hideAuthor(authorUid, name)
        loaders.forEach { it.refilter() }
    }

    fun report(loader: FeedLoader, summary: PublishedListSummary, reason: ReportReason, note: String?) {
        preferences.hideList(summary.id, summary.title)
        loader.refilter(loader.pane.value.justHidden + (summary.id to true))
        scope.launch {
            community.report(summary.id, reason, note).onFailure {
                Timber.w(it, "Could not file the report")
                preferences.unhideList(summary.id)
                loader.refilter(loader.pane.value.justHidden - summary.id)
                failed()
            }
        }
    }
}
