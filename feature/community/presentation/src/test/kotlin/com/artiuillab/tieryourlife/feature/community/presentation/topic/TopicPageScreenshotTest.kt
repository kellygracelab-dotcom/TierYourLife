package com.artiuillab.tieryourlife.feature.community.presentation.topic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.presentation.feed.CommunityFeed
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val PICTURE = "https://p/pokemon.webp"

private val Bands = listOf("#B03A32", "#C06A25", "#A98B1F", "#3F7F55", "#3C6E99")

private fun card(id: String, title: String, author: String, items: Int) = PublishedListSummary(
    id = id,
    title = title,
    authorUid = "u-$author",
    authorName = author,
    category = ListCategory.Games.id,
    itemCount = items,
    tierColors = Bands,
    updatedAtMillis = 0,
    topicId = "t1",
    topicName = "Pokémon",
)

private val Lists = listOf(
    card("1", "Every starter, ranked", "mariia", 27),
    card("2", "Gym leaders, no pictures needed", "kyle", 8),
    card("3", "Legendaries by design", "taras", 40),
    card("4", "Routes I'd walk again", "olena", 15),
)

/**
 * A topic's page is the section's page without the topics (Danylo, 15.11):
 * the same header, Follow, heading, sort and grid, in every window shape.
 */
@OptIn(ExperimentalCoilApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp-xhdpi")
class TopicPageScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val pictures = AsyncImagePreviewHandler { ColorImage(0xFFE0A030.toInt()) }

    private val topic = Topic("t1", "Pokémon", ListCategory.Games.id, TopicStatus.Approved, followerCount = 210, pictureUrl = PICTURE, listCount = 4)

    private val pokemon = TopicUiState(
        name = "Pokémon",
        section = ListCategory.Games.id,
        picture = PICTURE,
        topic = topic,
        feed = CommunityFeed.Ready(Lists),
    )

    private fun capture(name: String, state: TopicUiState = pokemon, dark: Boolean = false) {
        composeRule.setContent {
            val window = LocalWindowInfo.current.containerSize
            val shape = with(LocalDensity.current) { WindowShape.of(window.width.toDp(), window.height.toDp()) }
            TierYourLifeTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalWindowShape provides shape,
                    LocalInspectionMode provides true,
                    LocalAsyncImagePreviewHandler provides pictures,
                ) { Page(state) }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    @Composable
    private fun Page(state: TopicUiState) = TopicScreenContent(state = state, onBack = {})

    @Test
    fun light() {
        capture("topic_page_light")

        composeRule.onNodeWithText("Tier lists · 4", ignoreCase = true).assertExists()
        composeRule.onNodeWithText("Follow").assertExists()
    }

    @Test
    fun dark() = capture("topic_page_dark", dark = true)

    @Test
    fun withoutAPictureOfItsOwn_wearsTheSectionsArt() = capture("topic_page_no_picture", pokemon.copy(picture = null, topic = topic.copy(pictureUrl = null)))

    @Test
    fun waitsAsTheSectionDoes() = capture("topic_page_loading", pokemon.copy(feed = CommunityFeed.Loading))

    @Test
    fun withNothingInIt() = capture("topic_page_empty", pokemon.copy(topic = topic.copy(listCount = 0), feed = CommunityFeed.Ready(emptyList())))

    @Test
    fun aWaitingTopic_hasNothingToFollow() {
        capture("topic_page_waiting", pokemon.copy(picture = null, section = null, topic = topic.copy(section = null, status = TopicStatus.Pending, pictureUrl = null)))

        composeRule.onNodeWithText("Follow").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "en-w891dp-h411dp-land-xhdpi")
    fun phoneLandscape() = capture("topic_page_landscape")

    @Test
    @Config(qualifiers = "en-w1280dp-h800dp-land-mdpi")
    fun tablet() = capture("topic_page_tablet")

    @Test
    @Config(qualifiers = "en-w360dp-h380dp-xhdpi")
    fun flipCover() = capture("topic_page_cover")

    @Test
    @Config(qualifiers = "en-w360dp-h780dp-xhdpi")
    fun flipOpen() = capture("topic_page_flip_open", dark = true)

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
