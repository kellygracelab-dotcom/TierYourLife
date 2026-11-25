package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ErrorResult
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchNotice
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkPicture
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkRefusal
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components.LinkChoice
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components.ResultsGrid
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

/** The canvas row "Поиск картинок": a scope chosen by the board, an empty scope's way out, the web's day used up. */
@OptIn(ExperimentalCoilApi::class)
private val StillLoading = AsyncImagePreviewHandler { _, _ -> AsyncImagePainter.State.Loading(null) }

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class PictureSearchScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            // The pictures are still on their way: an address that cannot load here would take its tile away.
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalAsyncImagePreviewHandler provides StillLoading,
            ) {
                TierYourLifeTheme(darkTheme = dark) {
                    Box(Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) { content() }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    @OptIn(ExperimentalCoilApi::class)
    @Test
    fun aPictureThatWillNotLoad_takesItsTileAway() {
        val broken = AsyncImagePreviewHandler { _, request ->
            if (request.data == "https://a/c17.jpg") {
                AsyncImagePainter.State.Error(null, ErrorResult(null, request, IllegalStateException("never loads")))
            } else {
                AsyncImagePainter.State.Loading(null)
            }
        }
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalAsyncImagePreviewHandler provides broken) {
                TierYourLifeTheme { ResultsGrid(items = naruto(), selectedIds = emptySet(), onToggle = {}) }
            }
        }

        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult("anilist:c17")).assertDoesNotExist()
        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult("anilist:20")).assertExists()
    }

    private fun naruto() = listOf(
        CatalogueItem("anilist:20", "Naruto", "2002", "https://a/20.jpg"),
        CatalogueItem("anilist:c17", "Naruto Uzumaki", "Naruto", "https://a/c17.jpg"),
        CatalogueItem("anilist:1735", "Naruto: Shippuden", "2007", "https://a/1735.jpg"),
        CatalogueItem("anilist:c13", "Sasuke Uchiha", "Naruto", "https://a/c13.jpg"),
        CatalogueItem("anilist:97938", "Boruto: Naruto Next Generations", "2017", "https://a/97938.jpg"),
        CatalogueItem("anilist:c85", "Kakashi Hatake", "Naruto", "https://a/c85.jpg"),
    )

    @Test
    fun aBoardInAnime_searchesAnime() = capture("picture_search_scoped") {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Success(items = naruto()),
            query = "naruto",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Аниме 2026",
            selectedIds = setOf("anilist:20"),
            scopes = ScopeChoice(first = PictureScope.Anime, selected = PictureScope.Anime, onSelect = {}),
        )
    }

    @Test
    fun nothingTypedYet_theOtherWaysInStandWhereTheResultsWillBe() = capture("add_your_own") { OwnWays() }

    @Test
    fun nothingTypedYet_dark() = capture("add_your_own_dark", dark = true) { OwnWays() }

    @Composable
    private fun OwnWays() = CatalogueSearchScreenContent(
        state = CatalogueSearchUiState.Initial,
        query = "",
        onQueryChange = {},
        onSearchClick = {},
        listTitle = "Sci-fi films",
        onOwnPhoto = {},
        onAddName = {},
    )

    @Test
    fun anEmptyScope_offersEverywhere() = capture("picture_search_empty_scope") {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Empty(query = "borscht"),
            query = "borscht",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "PS2 era games",
            scopes = ScopeChoice(first = PictureScope.Games, selected = PictureScope.Games, onSelect = {}),
        )
    }

    @Test
    fun theWebsDayIsUsedUp_saysSoInOneLine_dark() = capture("picture_search_web_limit_dark", dark = true) {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Success(items = naruto().take(3), notice = SearchNotice.WebDailyLimit),
            query = "naruto",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Every A24 film",
            scopes = ScopeChoice(first = PictureScope.All, selected = PictureScope.All, onSelect = {}),
        )
    }

    @Test
    fun aPhotoThatAsksForCredit_namesItsAuthorUnderTheResult() = capture("picture_search_credit") {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Success(
                items = listOf(
                    CatalogueItem("inaturalist:42069", "Red Fox", "Vulpes vulpes", "https://i/1.jpg", credit = "© Jane Doe, CC BY"),
                    CatalogueItem("inaturalist:41", "Gray Wolf", "Canis lupus", "https://i/2.jpg", credit = "© Ivan Petrenko, CC BY-SA"),
                    CatalogueItem("wikidata:Q8331", "Fox", "Wikidata", "https://i/3.jpg"),
                ),
            ),
            query = "fox",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Animals I Could Beat in a Fight",
            scopes = ScopeChoice(first = PictureScope.Animals, selected = PictureScope.Animals, onSelect = {}),
        )
    }

    @Test
    fun anAddressOnTheClipboard_isOfferedAboveTheScopes() = capture("picture_link_paste") {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Initial,
            query = "",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Studio Ghibli, ranked",
            scopes = ScopeChoice(first = PictureScope.All, selected = PictureScope.All, onSelect = {}),
            links = LinkChoice(state = null, pasteOffered = true),
        )
    }

    @Test
    fun aCheckedPicture_isReadyToAdd() = capture("picture_link_ready") {
        LinkCard(LinkPicture.Found("https://i.example.net/p/8812.jpg", PictureAttribution("web"), null))
    }

    @Test
    fun aPageInsteadOfAPicture_saysWhatToDo_dark() = capture("picture_link_page_dark", dark = true) {
        LinkCard(LinkPicture.Refused(LinkRefusal.NotAPicture), url = "https://example.com/films/totoro")
    }

    @Test
    fun theDaysLinks_areUsedUp_andUploadingStillWorks() = capture("picture_link_limit") {
        val threeAtNight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 0)
        }
        LinkCard(LinkPicture.Limited(threeAtNight.timeInMillis))
    }

    @Test
    fun eachResult_saysWhatItIs_andTheResultsSayWhoseTheyAre() = capture("picture_search_kinds") {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Success(
                items = listOf(
                    CatalogueItem("tmdb:tv:46260", "Naruto", "2002", "https://t/1.jpg", kind = "tv", source = "tmdb"),
                    CatalogueItem("anilist:20", "Naruto", "2002", "https://a/20.jpg", kind = "anime", source = "anilist"),
                    CatalogueItem("anilist:m30011", "Naruto", "1999", "https://a/m1.jpg", kind = "manga", source = "anilist"),
                    CatalogueItem("anilist:c17", "Naruto Uzumaki", "Naruto", "https://a/c17.jpg", kind = "character", source = "anilist"),
                    CatalogueItem("wikidata:Q356", "Naruto", null, "https://w/1.jpg", kind = "thing", source = "wikidata"),
                    CatalogueItem("web:x", "Naruto poster", "example.com", "https://x/1.jpg", kind = "web", source = "web"),
                ),
            ),
            query = "naruto",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Аниме 2026",
            scopes = ScopeChoice(first = PictureScope.All, selected = PictureScope.All, onSelect = {}),
        )
    }

    // Stage 1: the words as typed found nothing, so the server searched the right spelling; a rose is a plant.
    @Test
    fun resultsFoundUnderOtherWords_sayWhich() = capture("picture_search_searched_as") {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Success(
                items = listOf(
                    CatalogueItem("tmdb:157336", "Interstellar", "2014", "https://t/1.jpg", kind = "movie", source = "tmdb"),
                    CatalogueItem("inaturalist:47851", "Rose", "Rosa", "https://i/1.jpg", kind = "plant", source = "inaturalist"),
                ),
                searchedAs = "Interstellar",
            ),
            query = "Intersteller",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Films",
            scopes = ScopeChoice(first = PictureScope.All, selected = PictureScope.All, onSelect = {}),
        )
    }

    @Test
    fun aBusyCatalogue_isWaitedFor_andSaysUntilWhen() = capture("picture_search_waiting") {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Waiting(at(14, 5)),
            query = "naruto",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Аниме 2026",
            scopes = ScopeChoice(first = PictureScope.Anime, selected = PictureScope.Anime, onSelect = {}),
        )
    }

    @Test
    fun tooManySearches_stopTheSearch_untilTheTimeItSays_dark() = capture("picture_search_limit_dark", dark = true) {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Limited(at(15, 0)),
            query = "dune",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Sci-fi films",
            scopes = ScopeChoice(first = PictureScope.FilmTv, selected = PictureScope.FilmTv, onSelect = {}),
        )
    }

    private fun at(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }.timeInMillis

    @Composable
    private fun LinkCard(answer: LinkPicture, url: String = "https://i.example.net/p/8812.jpg") {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Initial,
            query = url,
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Studio Ghibli, ranked",
            scopes = ScopeChoice(first = PictureScope.All, selected = PictureScope.All, onSelect = {}),
            links = LinkChoice(state = LinkUiState(url, answer)),
        )
    }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
