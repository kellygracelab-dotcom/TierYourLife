package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.ui.AddPhotoIcon
import com.artiuillab.tieryourlife.core.theme.ui.PicturesIcon
import com.artiuillab.tieryourlife.core.theme.ui.SectionLabel
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchTestTags

/**
 * Before anything is typed: the other ways a card comes in, all in one place,
 * so nobody has to find the camera in the bar or the row in the settings.
 * A picture's link, a photo of one's own, or a name alone, which the field
 * above takes and Add makes into a card.
 */
@Composable
internal fun AddYourOwn(fill: Color, onPasteLink: () -> Unit, onOwnPhoto: (() -> Unit)?, onJustAName: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag(CatalogueSearchTestTags.ITEM_SEARCH_ADD_OWN),
    ) {
        SectionLabel(
            text = stringResource(R.string.item_search_add_own),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 6.dp),
        )
        OwnRow(
            title = stringResource(R.string.item_search_own_link),
            subtitle = stringResource(R.string.item_search_own_link_sub),
            fill = fill,
            onClick = onPasteLink,
            tag = CatalogueSearchTestTags.ITEM_SEARCH_OWN_LINK,
        ) { PicturesIcon() }
        if (onOwnPhoto != null) {
            OwnRow(
                title = stringResource(R.string.item_search_own_photo),
                subtitle = stringResource(R.string.item_search_own_photo_sub),
                fill = fill,
                onClick = onOwnPhoto,
                tag = CatalogueSearchTestTags.ITEM_SEARCH_OWN_PHOTO,
            ) { AddPhotoIcon() }
        }
        OwnRow(
            title = stringResource(R.string.item_search_own_name),
            subtitle = stringResource(R.string.item_search_own_name_sub),
            fill = fill,
            onClick = onJustAName,
            tag = CatalogueSearchTestTags.ITEM_SEARCH_OWN_NAME,
        ) {
            Text(text = NAME_GLYPH, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A letter pair rather than a drawn icon: the row is about typed words. */
private const val NAME_GLYPH = "Aa"

@Composable
private fun OwnRow(title: String, subtitle: String, fill: Color, onClick: () -> Unit, tag: String, icon: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .testTag(tag)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(fill),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
