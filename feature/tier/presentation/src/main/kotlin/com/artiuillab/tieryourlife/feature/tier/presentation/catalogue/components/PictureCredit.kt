package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchTestTags

/** Who a search result's picture is by, under its name; once it is a card on the board, only its menu says so. */
@Composable
internal fun PictureCredit(credit: String, modifier: Modifier = Modifier) {
    Text(
        text = credit,
        modifier = modifier.testTag(CatalogueSearchTestTags.ITEM_SEARCH_CREDIT),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
