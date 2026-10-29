package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.components.ListActionsSheet
import com.artiuillab.tieryourlife.feature.community.presentation.components.ReportDialog
import com.artiuillab.tieryourlife.feature.community.presentation.components.ReportSentDialog

/**
 * A card's long-press menu and what follows from it: the existing sheet with
 * its author, Hide and Report, then the report dialog and the note that it
 * was sent. One per feed, so hiding reaches the feed the card came from.
 * This person's own list offers neither; their own anonymous one has nothing
 * left to offer, so it opens no sheet.
 */
@Composable
internal fun FeedListMenus(
    target: PublishedListSummary?,
    onDismiss: () -> Unit,
    feed: FeedCallbacks,
    onOpenAuthor: (uid: String, name: String, photoUrl: String?) -> Unit,
) {
    var reportFor by remember { mutableStateOf<PublishedListSummary?>(null) }
    var reportedFrom by remember { mutableStateOf<PublishedListSummary?>(null) }

    target?.takeUnless { it.mine && it.editorial }?.let { summary ->
        ListActionsSheet(
            title = summary.title,
            authorName = summary.authorName.takeUnless { summary.editorial },
            authorPhotoUrl = summary.authorPhotoUrl,
            onDismiss = onDismiss,
            onOpenAuthor = if (summary.editorial) {
                null
            } else {
                {
                    onDismiss()
                    onOpenAuthor(summary.authorUid, summary.authorName, summary.authorPhotoUrl)
                }
            },
            onHide = {
                onDismiss()
                feed.onHideList(summary)
            }.takeUnless { summary.mine },
            onReport = {
                onDismiss()
                reportFor = summary
            }.takeUnless { summary.mine },
        )
    }
    reportFor?.let { summary ->
        ReportDialog(
            onDismiss = { reportFor = null },
            onSend = { reason, note ->
                reportFor = null
                feed.onReport(summary, reason, note)
                reportedFrom = summary
            },
        )
    }
    reportedFrom?.let { summary ->
        ReportSentDialog(
            authorName = summary.authorName.takeUnless { summary.editorial },
            onDismiss = { reportedFrom = null },
            onHideAuthor = {
                reportedFrom = null
                feed.onHideAuthor(summary.authorUid, summary.authorName)
            },
        )
    }
}
