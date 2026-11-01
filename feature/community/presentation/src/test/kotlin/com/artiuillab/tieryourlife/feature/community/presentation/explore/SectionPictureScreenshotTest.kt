package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.presentation.allsections.AllSectionsContent
import com.artiuillab.tieryourlife.feature.community.presentation.allsections.AllSectionsUiState
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingFeedUiState
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionScreenContent
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A colour per picture, so each one is seen where it lands. */
private val Paint = mapOf(
    "https://p/food.webp" to 0xFFB86B3C.toInt(),
    "https://p/games.webp" to 0xFF4C8A6A.toInt(),
    "https://p/music.webp" to 0xFF6A5AA8.toInt(),
)

/** Three of the nine with a picture of their own; the rest keep their own art. */
private val Pictured = Section.BUILT_IN.map { section ->
    val picture = when (section.id) {
        ListCategory.Food.id -> "https://p/food.webp"
        ListCategory.Games.id -> "https://p/games.webp"
        ListCategory.Music.id -> "https://p/music.webp"
        else -> null
    }
    section.copy(pictureUrl = picture)
}

/** A built-in with a picture from the server shows it on its tile and its header; one without falls back to its own art. */
@OptIn(ExperimentalCoilApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class SectionPictureScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val pictures = AsyncImagePreviewHandler { request -> ColorImage(Paint[request.data] ?: 0xFF000000.toInt()) }

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            val window = LocalWindowInfo.current.containerSize
            val shape = with(LocalDensity.current) { WindowShape.of(window.width.toDp(), window.height.toDp()) }
            TierYourLifeTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalWindowShape provides shape,
                    LocalInspectionMode provides true,
                    LocalAsyncImagePreviewHandler provides pictures,
                ) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    @Test
    fun explore_builtInsWithPictures() = capture("section_pictures_explore") {
        CommunityHomeContent(ExploreUiState(sections = Pictured, feed = FeedPane(phase = FeedPhase.Ready)), FollowingFeedUiState())
    }

    @Test
    fun allSections_builtInsWithPictures_dark() = capture("section_pictures_all_dark", dark = true) {
        AllSectionsContent(AllSectionsUiState(sections = Pictured), onBack = {})
    }

    @Test
    fun section_aBuiltInsHeader_showsItsPicture() = capture("section_pictures_header") {
        val games = Pictured.first { it.id == ListCategory.Games.id }
        SectionScreenContent(
            SectionUiState(sectionId = games.id, section = games, countsKnown = true, topicsKnown = true, feed = FeedPane(phase = FeedPhase.Ready)),
            onBack = {},
        )
    }

    @Test
    fun section_aBuiltInWithoutAPicture_keepsItsArt() = capture("section_pictures_header_fallback") {
        val books = Pictured.first { it.id == ListCategory.Books.id }
        SectionScreenContent(
            SectionUiState(sectionId = books.id, section = books, countsKnown = true, topicsKnown = true, feed = FeedPane(phase = FeedPhase.Ready)),
            onBack = {},
        )
    }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
