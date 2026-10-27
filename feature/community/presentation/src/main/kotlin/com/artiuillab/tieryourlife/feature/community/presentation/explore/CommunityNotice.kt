package com.artiuillab.tieryourlife.feature.community.presentation.explore

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/** What a Community screen says in its snackbar. */
sealed interface CommunityNotice {
    data class RefreshFailed(val showingFromMillis: Long) : CommunityNotice
    data object FollowFailed : CommunityNotice
    data class NowFollowing(val section: ListCategory) : CommunityNotice

    /** With Undo, which follows the section again. */
    data class Unfollowed(val section: ListCategory) : CommunityNotice
    data object ActionFailed : CommunityNotice
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
