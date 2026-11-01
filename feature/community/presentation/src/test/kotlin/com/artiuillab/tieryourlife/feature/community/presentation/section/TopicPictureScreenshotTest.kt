package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPane
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPhase
import com.artiuillab.tieryourlife.feature.community.presentation.following.FollowingScreenContent
import com.artiuillab.tieryourlife.feature.community.presentation.following.FollowingUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val Pictured = listOf(
    Topic("t1", "Pokémon", ListCategory.Games.id, TopicStatus.Approved, followerCount = 210, pictureUrl = "https://p/t1.webp"),
    Topic("t2", "Zelda", ListCategory.Games.id, TopicStatus.Approved, followerCount = 120, pictureUrl = "https://p/t2.webp"),
    Topic("t3", "Souls-likes", ListCategory.Games.id, TopicStatus.Approved, followerCount = 90),
)

/** A colour per picture, so each one is seen where it lands. */
private val Paint = mapOf("https://p/t1.webp" to 0xFFE0A030.toInt(), "https://p/t2.webp" to 0xFF3A7D5C.toInt())

/** Topics with a picture of their own show it, cropped, on their tiles and in Manage; one without keeps the section's art or the tinted square. */
@OptIn(ExperimentalCoilApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class TopicPictureScreenshotTest {

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

    private val games = SectionUiState(
        sectionId = ListCategory.Games.id,
        section = Section.builtIn(ListCategory.Games).copy(listCount = 812),
        countsKnown = true,
        topicsKnown = true,
        topics = Pictured,
        feed = FeedPane(phase = FeedPhase.Ready),
    )

    @Test
    fun tiles_light() {
        capture("topic_picture_tiles_light") { SectionScreenContent(games, onBack = {}) }

        composeRule.onAllNodesWithTag(SectionTestTags.TOPIC_PICTURE, useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun tiles_dark() = capture("topic_picture_tiles_dark", dark = true) { SectionScreenContent(games, onBack = {}) }

    @Test
    fun manage_showsEachTopicsPicture() = capture("topic_picture_manage") {
        FollowingScreenContent(
            FollowingUiState.Ready(authors = emptyList(), topics = Pictured),
            onBack = {},
            onOpenAuthor = { _, _, _ -> },
            onToggle = {},
            onRetry = {},
        )
    }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
