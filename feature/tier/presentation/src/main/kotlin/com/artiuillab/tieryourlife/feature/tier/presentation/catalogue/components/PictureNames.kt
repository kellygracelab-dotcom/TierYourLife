package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchUiState
import java.util.Date

/** What a picture is of, as the site's tile says it; Wikidata's things and the web's pages have no label worth it. */
@Composable
internal fun kindName(kind: String?): String? = when (kind) {
    "movie" -> stringResource(R.string.item_search_kind_movie)
    "tv" -> stringResource(R.string.item_search_kind_tv)
    "person" -> stringResource(R.string.item_search_kind_person)
    "anime" -> stringResource(R.string.item_search_kind_anime)
    "manga" -> stringResource(R.string.item_search_kind_manga)
    "character" -> stringResource(R.string.item_search_kind_character)
    "game" -> stringResource(R.string.item_search_kind_game)
    "artist" -> stringResource(R.string.item_search_kind_artist)
    "album" -> stringResource(R.string.item_search_kind_album)
    "book" -> stringResource(R.string.item_search_kind_book)
    "animal" -> stringResource(R.string.item_search_kind_animal)
    "plant" -> stringResource(R.string.item_search_kind_plant)
    else -> null
}

/** Catalogues by their own names; the web in words; one this build does not know goes unnamed. */
private val SOURCE_NAMES = mapOf(
    "tmdb" to "TMDB",
    "anilist" to "AniList",
    "steam" to "Steam",
    "igdb" to "IGDB.com",
    "openlibrary" to "Open Library",
    "wikidata" to "Wikidata",
    "inaturalist" to "iNaturalist",
)

/** "From TMDB, AniList": whose pictures these are, in the order they first come, as over the site's results. */
@Composable
internal fun SourcesLine(items: List<CatalogueItem>) {
    val web = stringResource(R.string.item_search_web_source)
    val names = items.mapNotNull { it.source }.distinct().mapNotNull { if (it == WEB) web else SOURCE_NAMES[it] }
    if (names.isEmpty()) return
    Text(
        text = stringResource(R.string.item_search_from_sources, names.joinToString(", ")),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp)
            .testTag(CatalogueSearchTestTags.ITEM_SEARCH_SOURCES),
    )
}

/** "Showing results for Interstellar": the words as typed found nothing, these are for the ones searched instead. */
@Composable
internal fun SearchedAsLine(searchedAs: String?) {
    if (searchedAs == null) return
    Text(
        text = stringResource(R.string.item_search_showing_for, searchedAs),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp)
            .testTag(CatalogueSearchTestTags.ITEM_SEARCH_SEARCHED_AS),
    )
}

/** A search standing still for the server's reason: a wait, the search limit, or a catalogue that stayed busy. */
@Composable
internal fun HeldMessage(state: CatalogueSearchUiState.Held, onRetry: () -> Unit) {
    val format = DateFormat.getTimeFormat(LocalContext.current)
    val at = { millis: Long -> format.format(Date(millis)) }
    // Placed as the empty results are: under the chips, not against them.
    Column {
        Spacer(Modifier.height(24.dp))
        when (state) {
            is CatalogueSearchUiState.Waiting -> CenteredMessage(text = stringResource(R.string.item_search_waiting, at(state.untilMillis)))
            is CatalogueSearchUiState.Limited -> CenteredMessage(text = stringResource(R.string.item_search_limit, at(state.untilMillis)))
            is CatalogueSearchUiState.Busy -> CenteredMessage(
                text = state.untilMillis?.let { stringResource(R.string.item_search_busy, at(it)) } ?: stringResource(R.string.item_search_busy_soon),
                actionLabel = stringResource(R.string.item_search_try_again),
                onAction = onRetry,
            )
        }
    }
}

private const val WEB = "web"
