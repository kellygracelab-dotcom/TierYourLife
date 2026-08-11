package com.artiuillab.tieryourlife.feature.tier.presentation.trash.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.relativeTimeText

@Composable
internal fun TrashList(
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
