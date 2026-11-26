package com.artiuillab.tieryourlife.feature.tier.presentation.trash.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.systemBarsContentPadding
import com.artiuillab.tieryourlife.core.theme.ui.DeleteIcon
import com.artiuillab.tieryourlife.core.theme.ui.RestoreIcon
import com.artiuillab.tieryourlife.feature.tier.board.components.ListArt
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashTestTags
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

@Composable
internal fun TrashList(
    entries: List<TrashEntry>,
    onRestoreList: (Long) -> Unit,
    onRemoveRequested: (TrashEntry.DeletedList) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().testTag(TrashTestTags.LIST), contentPadding = systemBarsContentPadding()) {
        items(entries, key = { TrashTestTags.row(it) }) { entry ->
            when (entry) {
                is TrashEntry.DeletedList -> TrashListRow(
                    entry = entry,
                    onRestore = { onRestoreList(entry.id) },
                    onRemove = { onRemoveRequested(entry) },
                )
            }
        }
    }
}

@Composable
private fun TrashListRow(entry: TrashEntry.DeletedList, onRestore: () -> Unit, onRemove: () -> Unit) {
    TrashRow(
        thumbnail = {
            ListArt(
                coverImageUrl = entry.coverImageUrl,
                previewImages = entry.pictures,
                tierColors = emptyList(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        },
        title = entry.title,
        meta = stringResource(
            R.string.trash_row_list_meta,
            pluralStringResource(ThemeR.plurals.list_items_count, entry.itemCount, entry.itemCount),
            relativeTimeText(entry.deletedAtMillis),
        ),
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

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // The word where the title keeps its room; on the narrowest phones the arrow, as before,
        // or "Restore" in Russian left the title four characters wide.
        val worded = maxWidth >= WORDED_FROM
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .testTag(TrashTestTags.row(entry))
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            thumbnail()
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val restoreTag = Modifier
                .semantics { contentDescription = restoreDescription }
                .testTag(TrashTestTags.restoreButton(entry))
            if (worded) {
                TextButton(onClick = onRestore, modifier = restoreTag) { Text(stringResource(R.string.trash_restore)) }
            } else {
                IconButton(onClick = onRestore, modifier = restoreTag) { RestoreIcon(22.dp, MaterialTheme.colorScheme.primary) }
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .semantics { contentDescription = removeDescription }
                    .testTag(TrashTestTags.removeButton(entry)),
            ) { DeleteIcon(22.dp, MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private val WORDED_FROM = 360.dp
