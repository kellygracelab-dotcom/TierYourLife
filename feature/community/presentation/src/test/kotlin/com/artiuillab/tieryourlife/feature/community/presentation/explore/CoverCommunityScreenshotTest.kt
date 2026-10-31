package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.core.theme.ui.CoverSurface
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
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

private const val COVER_STATUS_DP = 22

/** Samsung's dots (14dp) and the navigation bar (36dp). */
private const val COVER_BOTTOM_DP = 50

private val Bands = listOf("#B03A32", "#C06A25", "#A98B1F", "#3F7F55", "#3C6E99")

private fun game(id: String, title: String, author: String) = PublishedListSummary(
    id = id,
    title = title,
    authorUid = "u-$author",
    authorName = author,
    category = ListCategory.Games.id,
    itemCount = 20,
    tierColors = Bands,
    updatedAtMillis = 0,
)

private val games = SectionUiState(
    sectionId = ListCategory.Games.id,
    section = Section.builtIn(ListCategory.Games).copy(listCount = 812),
    countsKnown = true,
    feed = FeedPane(
        phase = FeedPhase.Ready,
        lists = listOf(
            game("1", "PS2 era games", "mariia"),
            game("2", "Every Pokémon starter", "nina"),
            game("3", "Gym leaders, no pictures needed", "kyle"),
            game("4", "Souls-likes by how fair they feel", "taras"),
            game("5", "Zelda, every one of them", "olena"),
            game("6", "Couch co-op that still works", "artiman"),
        ),
    ),
)

/**
 * Community on the Flip6's cover (§7, §8) at 352 x 339dp, with the status
 * bar and the dots and navigation bar drawn as bands.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w352dp-h339dp-xhdpi")
class CoverCommunityScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            val window = LocalWindowInfo.current.containerSize
            val shape = with(LocalDensity.current) { WindowShape.of(window.width.toDp(), window.height.toDp()) }
            TierYourLifeTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalWindowShape provides shape) {
                    Box {
                        CoverSurface { content() }
                        SystemBarBands(COVER_STATUS_DP, COVER_BOTTOM_DP)
                    }
                }
            }
        }
        composeRule.runOnUiThread { composeRule.activity.fakeSystemBars(COVER_STATUS_DP, COVER_BOTTOM_DP) }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    @Test
    fun theSections_light() = capture("cover_community_light", dark = false) { CoverCommunityScreen(onBack = {}, onOpenSection = {}) }

    @Test
    fun theSections_dark() = capture("cover_community_dark", dark = true) { CoverCommunityScreen(onBack = {}, onOpenSection = {}) }

    @Test
    fun aSection_light() = capture("cover_section_light", dark = false) { SectionScreenContent(games, onBack = {}) }

    @Test
    fun aSection_dark() = capture("cover_section_dark", dark = true) { SectionScreenContent(games, onBack = {}) }

    @Test
    fun aSectionThatCouldNotLoad() = capture("cover_section_failed", dark = false) {
        SectionScreenContent(games.copy(feed = FeedPane(phase = FeedPhase.Failed)), onBack = {})
    }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
