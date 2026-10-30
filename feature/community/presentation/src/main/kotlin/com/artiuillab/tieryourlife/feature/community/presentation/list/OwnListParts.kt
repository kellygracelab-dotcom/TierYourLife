package com.artiuillab.tieryourlife.feature.community.presentation.list

import android.content.ClipData
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.tier.board.share.SHARE_LINK
import kotlinx.coroutines.launch

/** What the page of their own list can do beyond opening the board. */
data class OwnListCallbacks(
    val onRestore: () -> Unit = {},
    val onGetBack: () -> Unit = {},
    val onTakeDown: () -> Unit = {},
    val onOpenMyPublished: () -> Unit = {},
    val onPublishAnonymously: (Boolean) -> Unit = {},
)

private val OWN_LINE_HEIGHT = 56.dp
private val CHIP_SHAPE = RoundedCornerShape(8.dp)

/** Said before anything else on their own list: whose it is. */
@Composable
internal fun YourListChip(modifier: Modifier = Modifier) {
    Surface(
        shape = CHIP_SHAPE,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.testTag(CommunityListTestTags.YOUR_LIST),
    ) {
        Text(
            text = stringResource(R.string.community_your_list_badge),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
        )
    }
}

/** Out under their name with how many have ranked it, or out without it, which only they can tell. */
@Composable
internal fun ownMeta(state: CommunityListUiState.Success): String = when {
    state.anonymous || state.byNobody -> stringResource(R.string.community_own_anonymous)
    state.rankingCount > 0 -> pluralStringResource(R.plurals.community_own_published_rankings, state.rankingCount, state.rankingCount)
    else -> stringResource(R.string.community_own_published)
}

/** The chip and the meta line, in the line the author's name takes on somebody else's list. */
@Composable
internal fun OwnListLine(state: CommunityListUiState.Success, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(OWN_LINE_HEIGHT)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        YourListChip()
        OwnMetaText(state)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun OwnMetaText(state: CommunityListUiState.Success, modifier: Modifier = Modifier) {
    Text(
        text = ownMeta(state),
        modifier = modifier.testTag(CommunityListTestTags.OWN_META),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Open, restore or rebuild: whichever the board behind the list needs. */
@Composable
internal fun OwnBoardButton(
    state: CommunityListUiState.Success,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp? = null,
    maxLines: Int = 2,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp),
) {
    val action = state.ownBoardAction ?: return
    val (text, tag) = when (action) {
        OwnBoardAction.Open -> R.string.community_open_your_board to CommunityListTestTags.OPEN_BOARD
        OwnBoardAction.Restore -> R.string.community_restore_from_trash to CommunityListTestTags.RESTORE_BOARD
        OwnBoardAction.GetBack -> R.string.community_get_back_as_board to CommunityListTestTags.GET_BACK_BOARD
    }
    Button(
        onClick = onClick,
        enabled = !state.ownBusy,
        contentPadding = contentPadding,
        modifier = modifier
            .then(if (height != null) Modifier.height(height) else Modifier.heightIn(min = 40.dp))
            .widthIn(max = OWN_BUTTON_MAX_WIDTH)
            .testTag(tag),
    ) {
        Text(text = stringResource(text), textAlign = TextAlign.Center, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun CopyLinkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp),
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = contentPadding,
        modifier = modifier
            .then(if (height != null) Modifier.height(height) else Modifier)
            .testTag(CommunityListTestTags.COPY_LINK),
    ) {
        Text(text = stringResource(R.string.community_copy_link), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private val OWN_BUTTON_MAX_WIDTH = 180.dp

/** The list's address on the web, the one the site itself copies. */
internal fun listLink(publishedId: String): String = "$SHARE_LINK/l/${Uri.encode(publishedId)}"

/** Puts the link on the clipboard; Android 13 and later say so themselves, earlier ones get the snackbar. */
@Composable
internal fun rememberCopyLink(publishedId: String, snackbars: SnackbarHostState): () -> Unit {
    val clipboard = LocalClipboard.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            val link = listLink(publishedId)
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(link, link)))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                snackbars.showSnackbar(resources.getString(R.string.community_link_copied))
            }
        }
    }
}
