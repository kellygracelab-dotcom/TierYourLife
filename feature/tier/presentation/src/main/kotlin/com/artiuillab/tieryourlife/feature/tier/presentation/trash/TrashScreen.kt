package com.artiuillab.tieryourlife.feature.tier.presentation.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.TierYourLifeMedia
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.DeleteSweepIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.common.MoreIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeleteOutlineIcon

internal object TrashTestTags {
    const val BACK = "trash_back"
    const val MORE = "trash_more"
    const val MENU_EMPTY_TRASH = "trash_menu_empty_trash"
    const val LIST = "trash_list"
    const val EMPTY_STATE = "trash_empty_state"
    const val REMOVE_DIALOG = "trash_remove_dialog"
    const val REMOVE_CONFIRM = "trash_remove_confirm"
    const val REMOVE_CANCEL = "trash_remove_cancel"
    const val EMPTY_TRASH_DIALOG = "trash_empty_trash_dialog"
    const val EMPTY_TRASH_CONFIRM = "trash_empty_trash_confirm"
    const val EMPTY_TRASH_CANCEL = "trash_empty_trash_cancel"
    fun row(entry: TrashEntry): String = when (entry) {
        is TrashEntry.DeletedList -> "trash_row_list_${entry.id}"
        is TrashEntry.DeletedItem -> "trash_row_item_${entry.id}"
    }
    fun restoreButton(entry: TrashEntry): String = "${row(entry)}_restore"
    fun removeButton(entry: TrashEntry): String = "${row(entry)}_remove"
}

@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    TrashScreenContent(
        state = state,
        onBack = onBack,
        onRestoreList = viewModel::restoreList,
        onRestoreItem = viewModel::restoreItem,
        onRemoveListPermanently = viewModel::removeListPermanently,
        onRemoveItemPermanently = viewModel::removeItemPermanently,
        onEmptyTrash = viewModel::emptyTrash,
    )
}

@Composable
internal fun TrashScreenContent(
    state: TrashUiState,
    onBack: () -> Unit,
    onRestoreList: (Long) -> Unit = {},
    onRestoreItem: (Long) -> Unit = {},
    onRemoveListPermanently: (Long) -> Unit = {},
    onRemoveItemPermanently: (Long) -> Unit = {},
    onEmptyTrash: () -> Unit = {},
) {
    val entries = (state as? TrashUiState.Success)?.entries.orEmpty()
    var menuExpanded by remember { mutableStateOf(false) }
    var emptyDialogVisible by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<TrashEntry?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize()) {
            TrashTopBar(
                isEmpty = entries.isEmpty(),
                menuExpanded = menuExpanded,
                onBack = onBack,
                onMoreClick = { menuExpanded = true },
                onDismissMenu = { menuExpanded = false },
                onEmptyTrashClick = {
                    menuExpanded = false
                    emptyDialogVisible = true
                },
            )

            when (state) {
                TrashUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                is TrashUiState.Error -> Text(
                    text = state.message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                is TrashUiState.Success -> if (entries.isEmpty()) {
                    TrashEmptyState()
                } else {
                    TrashList(
                        entries = entries,
                        onRestoreList = onRestoreList,
                        onRestoreItem = onRestoreItem,
                        onRemoveRequested = { removeTarget = it },
                    )
                }
            }
        }
    }

    val target = removeTarget
    if (target != null) {
        RemoveConfirmDialog(
            entry = target,
            onDismiss = { removeTarget = null },
            onConfirm = {
                when (target) {
                    is TrashEntry.DeletedList -> onRemoveListPermanently(target.id)
                    is TrashEntry.DeletedItem -> onRemoveItemPermanently(target.id)
                }
                removeTarget = null
            },
        )
    }

    if (emptyDialogVisible) {
        EmptyTrashDialog(
            entryCount = entries.size,
            onDismiss = { emptyDialogVisible = false },
            onConfirm = {
                onEmptyTrash()
                emptyDialogVisible = false
            },
        )
    }
}

@Composable
private fun TrashTopBar(
    isEmpty: Boolean,
    menuExpanded: Boolean,
    onBack: () -> Unit,
    onMoreClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onEmptyTrashClick: () -> Unit,
) {
    val backDescription = stringResource(R.string.cd_trash_back)
    val moreDescription = stringResource(R.string.cd_more)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = backDescription }
                    .testTag(TrashTestTags.BACK),
            ) { BackIcon() }

            // Everything else in the bar — title and more_vert — is absent, not just
            // hidden, when the trash is empty (docs/design-spec-home.md, section 6).
            if (!isEmpty) {
                Text(
                    text = stringResource(R.string.trash_title),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box {
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = moreDescription }
                            .testTag(TrashTestTags.MORE),
                    ) { MoreIcon() }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.menu_empty_trash),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            leadingIcon = { DeleteSweepIcon(20.dp, MaterialTheme.colorScheme.error) },
                            onClick = onEmptyTrashClick,
                            modifier = Modifier.testTag(TrashTestTags.MENU_EMPTY_TRASH),
                        )
                    }
                }
            }
        }

        if (!isEmpty) {
            Text(
                text = stringResource(R.string.trash_subtitle),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrashList(
    entries: List<TrashEntry>,
    onRestoreList: (Long) -> Unit,
    onRestoreItem: (Long) -> Unit,
    onRemoveRequested: (TrashEntry) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().testTag(TrashTestTags.LIST)) {
        items(entries, key = { TrashTestTags.row(it) }) { entry ->
            when (entry) {
                is TrashEntry.DeletedList -> TrashListRow(
                    entry = entry,
                    onRestore = { onRestoreList(entry.id) },
                    onRemove = { onRemoveRequested(entry) },
                )

                is TrashEntry.DeletedItem -> TrashItemRow(
                    entry = entry,
                    onRestore = { onRestoreItem(entry.id) },
                    onRemove = { onRemoveRequested(entry) },
                )
            }
        }
    }
}

@Composable
private fun TrashListRow(entry: TrashEntry.DeletedList, onRestore: () -> Unit, onRemove: () -> Unit) {
    TrashRow(
        thumbnail = { ListThumbnail() },
        title = entry.title,
        meta = stringResource(
            R.string.trash_row_list_meta,
            pluralStringResource(R.plurals.list_items_count, entry.itemCount, entry.itemCount),
            relativeTimeText(entry.deletedAtMillis),
        ),
        entry = entry,
        onRestore = onRestore,
        onRemove = onRemove,
    )
}

@Composable
private fun TrashItemRow(entry: TrashEntry.DeletedItem, onRestore: () -> Unit, onRemove: () -> Unit) {
    TrashRow(
        thumbnail = { ItemThumbnail(entry.imageUrl) },
        title = entry.title,
        meta = stringResource(R.string.trash_row_item_meta, entry.listTitle, relativeTimeText(entry.deletedAtMillis)),
        entry = entry,
        onRestore = onRestore,
        onRemove = onRemove,
    )
}

@Composable
private fun TrashRow(
    thumbnail: @Composable () -> Unit,
    title: String,
    meta: String,
    entry: TrashEntry,
    onRestore: () -> Unit,
    onRemove: () -> Unit,
) {
    val restoreDescription = stringResource(R.string.cd_restore, title)
    val removeDescription = stringResource(R.string.cd_remove, title)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .testTag(TrashTestTags.row(entry))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        thumbnail()
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(text = meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(
            onClick = onRestore,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .semantics { contentDescription = restoreDescription }
                .testTag(TrashTestTags.restoreButton(entry)),
        ) { Text(stringResource(R.string.action_restore)) }
        TextButton(
            onClick = onRemove,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .semantics { contentDescription = removeDescription }
                .testTag(TrashTestTags.removeButton(entry)),
        ) { Text(stringResource(R.string.action_remove)) }
    }
}

// A miniature of the distribution bar — the S band, the A band and the unranked colour
// in equal thirds — so a deleted list is recognisable as a list at a glance. The trash
// entry carries no per-tier colours of its own, so this is decorative rather than data-
// driven (docs/design-spec-home.md, section 6).
@Composable
private fun ListThumbnail() {
    val media = TierYourLifeMedia.current
    val sBand = tierRowColors("#B03A32", "#F1948C").band
    val aBand = tierRowColors("#C06A25", "#E9A867").band
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(media.tilePlaceholder)
            .padding(6.dp),
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(sBand, aBand, media.unrankedRibbon).forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun ItemThumbnail(imageUrl: String?) {
    val media = TierYourLifeMedia.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(media.tilePlaceholder),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun RemoveConfirmDialog(entry: TrashEntry, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val body = when (entry) {
        is TrashEntry.DeletedList -> stringResource(
            R.string.remove_dialog_body_list,
            entry.title,
            pluralStringResource(R.plurals.list_items_count, entry.itemCount, entry.itemCount),
        )

        is TrashEntry.DeletedItem -> stringResource(R.string.remove_dialog_body_item, entry.title)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TrashTestTags.REMOVE_DIALOG),
        title = { Text(stringResource(R.string.remove_dialog_title), style = MaterialTheme.typography.headlineSmall) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag(TrashTestTags.REMOVE_CONFIRM),
            ) { Text(stringResource(R.string.action_remove)) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TrashTestTags.REMOVE_CANCEL),
            ) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun EmptyTrashDialog(entryCount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    // Read when the dialog opens, so the count in the body can never disagree with the
    // list behind it (docs/design-spec-home.md, section 6).
    val entryCountText = pluralStringResource(R.plurals.trash_entry_count, entryCount, entryCount)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TrashTestTags.EMPTY_TRASH_DIALOG),
        title = {
            Text(stringResource(R.string.empty_trash_dialog_title), style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Text(
                text = stringResource(R.string.empty_trash_dialog_body, entryCountText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag(TrashTestTags.EMPTY_TRASH_CONFIRM),
            ) { Text(stringResource(R.string.action_empty_trash)) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TrashTestTags.EMPTY_TRASH_CANCEL),
            ) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun TrashEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TrashTestTags.EMPTY_STATE),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 264.dp).padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                contentAlignment = Alignment.Center,
            ) { DeleteOutlineIcon(28.dp, MaterialTheme.colorScheme.outline) }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.trash_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.trash_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
