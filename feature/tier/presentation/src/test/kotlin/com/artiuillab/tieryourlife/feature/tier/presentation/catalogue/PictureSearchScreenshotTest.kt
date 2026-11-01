package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchNotice
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The canvas row "Поиск картинок": a scope chosen by the board, an empty scope's way out, the web's day used up. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class PictureSearchScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            TierYourLifeTheme(darkTheme = dark) {
                Box(Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) { content() }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
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

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
