package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.board.components.ListArt
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.summary
import com.artiuillab.tieryourlife.feature.tier.presentation.R

/** What the cover is made of, as the sheet names it. */
internal enum class CoverKind { Auto, Colours, Collage, CardPicture, Photo }

internal fun TierList.coverKind(): CoverKind {
    val one = coverImageUrl
    return when {
        one != null && tiers.any { tier -> tier.items.any { it.imageUrl == one } } -> CoverKind.CardPicture
        one != null -> CoverKind.Photo
        coverImageUrls.isNotEmpty() -> CoverKind.Collage
        tiers.any { tier -> tier.items.any { it.imageUrl != null } } -> CoverKind.Auto
        else -> CoverKind.Colours
    }
}

/**
 * The cover as Community will show it, what it is made of, and Change, which
 * opens the cover picker (Danylo, 16.11.2026: a collage by default, plainly
 * one's own to change).
 */
@Composable
internal fun CoverBlock(list: TierList, enabled: Boolean, onChange: () -> Unit) {
    val art = list.summary()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ListArt(
                coverImageUrl = art.coverImageUrl,
                previewImages = art.coverImageUrls.ifEmpty { art.previewImages },
                tierColors = list.tiers.filterNot { it.isPool }.map { it.colorLight },
                modifier = Modifier.width(112.dp).aspectRatio(16f / 10f).clip(RoundedCornerShape(10.dp)),
                testTag = PublishSheetTags.COVER,
                coverFocus = art.coverFocus,
                pictureFocus = art.pictureFocus,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(coverKindLabel(list), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                val description = stringResource(R.string.publish_sheet_cover_change)
                OutlinedButton(
                    onClick = onChange,
                    enabled = enabled,
                    modifier = Modifier.testTag(PublishSheetTags.CHANGE_COVER).semantics(mergeDescendants = true) { contentDescription = description },
                ) {
                    Text(stringResource(R.string.publish_sheet_change), Modifier.clearAndSetSemantics {})
                }
            }
        }
        Text(
            stringResource(R.string.publish_sheet_cover_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun coverKindLabel(list: TierList): String = when (list.coverKind()) {
    CoverKind.Auto -> stringResource(R.string.publish_sheet_cover_auto)
    CoverKind.Colours -> stringResource(R.string.publish_sheet_cover_colours)
    CoverKind.Collage -> list.coverImageUrls.size.let { pluralStringResource(R.plurals.publish_sheet_cover_collage, it, it) }
    CoverKind.CardPicture -> stringResource(R.string.publish_sheet_cover_card)
    CoverKind.Photo -> stringResource(R.string.publish_sheet_cover_photo)
}
