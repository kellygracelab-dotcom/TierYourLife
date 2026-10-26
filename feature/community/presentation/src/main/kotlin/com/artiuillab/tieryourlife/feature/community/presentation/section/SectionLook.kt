package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory

/**
 * A section's four colours, light and dark, from one tint mixed in OKLab
 * (docs/design-spec-community-explore.md §12). The tile art does not change
 * with the theme; only what shows before and around it does.
 */
internal data class SectionLook(
    val tint: Color,
    val tintDark: Color,
    val topicBg: Color,
    val topicBgDark: Color,
    val topicIcon: Color,
    val topicIconDark: Color,
)

internal fun lookOf(section: ListCategory): SectionLook = when (section) {
    ListCategory.Anime -> AnimeLook
    ListCategory.FilmTv -> FilmTvLook
    ListCategory.Games -> GamesLook
    ListCategory.Music -> MusicLook
    ListCategory.Books -> BooksLook
    ListCategory.Food -> FoodLook
    ListCategory.Sport -> SportLook
    ListCategory.People -> PeopleLook
    ListCategory.Other -> OtherLook
}

private val AnimeLook = SectionLook(
    Color(0xFF7E4E74),
    Color(0xFF64405E),
    Color(0xFFDFD0DF),
    Color(0xFF322533),
    Color(0xFF7E4E74),
    Color(0xFFE7B0DA),
)

private val FilmTvLook = SectionLook(
    Color(0xFF3E4A6C),
    Color(0xFF343D58),
    Color(0xFFCECFDD),
    Color(0xFF1F2431),
    Color(0xFF3E4A6C),
    Color(0xFFB4C3EC),
)

private val GamesLook = SectionLook(
    Color(0xFF3D6B58),
    Color(0xFF345649),
    Color(0xFFCFD8D8),
    Color(0xFF202D2B),
    Color(0xFF3D6B58),
    Color(0xFFA0D1BB),
)

private val MusicLook = SectionLook(
    Color(0xFF62508A),
    Color(0xFF4F426F),
    Color(0xFFD7D1E5),
    Color(0xFF292539),
    Color(0xFF62508A),
    Color(0xFFCAB8FA),
)

private val BooksLook = SectionLook(
    Color(0xFF7A5B3F),
    Color(0xFF614A37),
    Color(0xFFDED4D2),
    Color(0xFF302925),
    Color(0xFF7A5B3F),
    Color(0xFFE0BD9E),
)

private val FoodLook = SectionLook(
    Color(0xFF9A5638),
    Color(0xFF794732),
    Color(0xFFE7D3D1),
    Color(0xFF3A2824),
    Color(0xFF9A5638),
    Color(0xFFFBAF8F),
)

private val SportLook = SectionLook(
    Color(0xFF2F6A76),
    Color(0xFF2A5560),
    Color(0xFFCDD7DF),
    Color(0xFF1D2D34),
    Color(0xFF2F6A76),
    Color(0xFF94D0DD),
)

private val PeopleLook = SectionLook(
    Color(0xFF76586A),
    Color(0xFF5E4857),
    Color(0xFFDDD3DC),
    Color(0xFF2F2830),
    Color(0xFF76586A),
    Color(0xFFDBB9CD),
)

private val OtherLook = SectionLook(
    Color(0xFF55566A),
    Color(0xFF454656),
    Color(0xFFD4D2DC),
    Color(0xFF262730),
    Color(0xFF55566A),
    Color(0xFFC0C2D9),
)

/** Read off the scheme rather than passed down: every screen here is drawn inside the app's theme. */
internal val inDarkTheme: Boolean
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE

private const val DARK_SURFACE_LUMINANCE = 0.5f

internal val SectionLook.tintNow: Color
    @Composable
    @ReadOnlyComposable
    get() = if (inDarkTheme) tintDark else tint

internal val SectionLook.topicBgNow: Color
    @Composable
    @ReadOnlyComposable
    get() = if (inDarkTheme) topicBgDark else topicBg

internal val SectionLook.topicIconNow: Color
    @Composable
    @ReadOnlyComposable
    get() = if (inDarkTheme) topicIconDark else topicIcon

/** On section art, the same in both themes. */
internal val SectionLabel = Color.White
internal val SectionNewLine = Color.White.copy(alpha = 0.88f)
internal val TileScrimTop = Color.Black.copy(alpha = 0.05f)
internal val TileScrimBottom = Color.Black.copy(alpha = 0.62f)
