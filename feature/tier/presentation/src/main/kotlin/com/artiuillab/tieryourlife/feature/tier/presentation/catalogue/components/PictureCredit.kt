package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
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

/**
 * Whom a result's picture names, short, as the site has it (artboard 23):
 * "Rosino, CC BY-SA". The whole line stays in About the picture; a bare
 * address is never shown as a name.
 */
internal fun CatalogueItem.shortCredit(): String? {
    val author = attribution?.author?.trim().orEmpty()
    if (author.isNotEmpty()) {
        val license = attribution?.license?.replace(LICENSE_VERSION, "")?.trim()
        return listOfNotNull(author, license?.takeIf { it.isNotEmpty() }).joinToString(", ")
    }
    val whole = credit?.trim().orEmpty()
    return whole.takeUnless { it.isEmpty() || BARE_ADDRESS.containsMatchIn(it) }
}

private val LICENSE_VERSION = Regex("""\s+v?\d+(\.\d+)*$""")
private val BARE_ADDRESS = Regex("^(https?:)?//", RegexOption.IGNORE_CASE)
