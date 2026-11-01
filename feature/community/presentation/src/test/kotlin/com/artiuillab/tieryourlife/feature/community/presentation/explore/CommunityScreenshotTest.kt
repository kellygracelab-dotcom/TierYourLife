package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedSection
import com.artiuillab.tieryourlife.feature.community.domain.model.Following
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionSymbols
import com.artiuillab.tieryourlife.feature.community.domain.model.ThemePair
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.presentation.allsections.AllSectionsContent
import com.artiuillab.tieryourlife.feature.community.presentation.allsections.AllSectionsUiState
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingFeedUiState
import com.artiuillab.tieryourlife.feature.community.presentation.following.FollowKeys
import com.artiuillab.tieryourlife.feature.community.presentation.following.FollowingScreenContent
import com.artiuillab.tieryourlife.feature.community.presentation.following.FollowingUiState
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionScreenContent
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionUiState
import com.artiuillab.tieryourlife.feature.tier.board.common.KnownSections
import com.artiuillab.tieryourlife.feature.tier.board.common.LocalSections
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val Bands = listOf("#B03A32", "#C06A25", "#A98B1F", "#3F7F55", "#3C6E99")

private fun card(id: String, title: String, author: String, category: String, items: Int, topic: Pair<String, String>? = null) =
    PublishedListSummary(
        id = id,
        title = title,
        authorUid = "u-$author",
        authorName = author,
        category = category,
        itemCount = items,
        tierColors = Bands,
        updatedAtMillis = 0,
        topicId = topic?.first,
        topicName = topic?.second,
    )

private val Popular = listOf(
    card("1", "Every A24 film, ranked", "danylo", ListCategory.FilmTv.id, 34),
    card("2", "PS2 era games", "mariia", ListCategory.Games.id, 28),
    card("3", "Coffee in Lviv", "taras", ListCategory.Food.id, 12),
    card("4", "Albums I put on to work", "olena", ListCategory.Music.id, 41),
    card("5", "Gym leaders, no pictures needed", "kyle", ListCategory.Games.id, 8),
    card("6", "Space films that hold up", "artiman", ListCategory.FilmTv.id, 12),
)

private val Counted = Section.BUILT_IN.mapIndexed { i, section ->
    section.copy(listCount = 100 + i * 37)
}

private val Tints = listOf(
    "#7A4A33" to "#3A2A26", "#2F5D7A" to "#26303A", "#5B6B2F" to "#2E3326", "#7A3355" to "#3A2630", "#4A3F7A" to "#2C2A3A",
)

/** Somebody's sections, one for each icon a proposer may pick, with tints as the server would compute them and no picture to load. */
private val Theirs = SectionSymbols.PROPOSABLE.mapIndexed { i, icon ->
    val (light, dark) = Tints[i % Tints.size]
    Section(
        id = "s-$icon",
        name = icon.replace('_', ' ').replaceFirstChar { it.uppercase() },
        icon = icon,
        tint = ThemePair(light, dark),
        listCount = 300 - i * 11,
        lists30d = 40 - i,
        approvedAt = 1_790_000_000_000L + i,
    )
}

/** The nine and five more, as the server orders them: busiest first, Other last. */
private val Fourteen = Theirs.take(5) + Counted

private const val NOW = 1_793_000_000_000L

/**
 * Community as it looks in each window the spec draws (§2, §4, §6.1, §7.1,
 * §8), compared with the pictures beside the tests. A change that moves a
 * pixel on purpose re-records them (`recordRoborazziDebug`).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class CommunityScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            val window = LocalWindowInfo.current.containerSize
            val shape = with(LocalDensity.current) { WindowShape.of(window.width.toDp(), window.height.toDp()) }
            TierYourLifeTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalWindowShape provides shape) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    private val explore = ExploreUiState(sections = Counted, feed = FeedPane(lists = Popular, phase = FeedPhase.Ready), nowMillis = NOW)

    private val following = FollowingFeedUiState(
        following = Following(
            authors = listOf(FollowedAuthor("u-mariia", "mariia", null), FollowedAuthor("u-kyle", "kyle", null)),
            topics = listOf(Topic("t1", "Pokémon", ListCategory.Games.id, TopicStatus.Approved, followerCount = 210)),
            sections = listOf(FollowedSection(ListCategory.Food.id), FollowedSection(ListCategory.FilmTv.id)),
        ),
        waiting = listOf(Topic("w1", "Shiny hunts", ListCategory.Games.id, TopicStatus.Pending)),
        feed = FeedPane(
            phase = FeedPhase.Ready,
            lists = listOf(
                card("2", "PS2 era games", "mariia", ListCategory.Games.id, 28),
                card("7", "Every Pokémon starter", "nina", ListCategory.Games.id, 27, topic = "t1" to "Pokémon"),
                card("3", "Coffee in Lviv", "taras", ListCategory.Food.id, 12),
                card("6", "Space films that hold up", "artiman", ListCategory.FilmTv.id, 12),
            ),
        ),
        nowMillis = NOW,
    )

    @Test
    fun explore_light() = capture("explore_light") { CommunityHomeContent(explore, FollowingFeedUiState()) }

    @Test
    fun explore_dark() = capture("explore_dark", dark = true) { CommunityHomeContent(explore, FollowingFeedUiState()) }

    @Test
    fun explore_firstOpen() = capture("explore_first_open") {
        CommunityHomeContent(explore.copy(feed = FeedPane()), FollowingFeedUiState())
    }

    @Test
    fun explore_failedWithNothingKept() = capture("explore_failed") {
        CommunityHomeContent(explore.copy(feed = FeedPane(phase = FeedPhase.Failed)), FollowingFeedUiState())
    }

    @Test
    fun explore_offlineWithAnOldFeedAndNewListsWaiting() = capture("explore_offline") {
        CommunityHomeContent(
            explore.copy(feed = explore.feed.copy(offlineSinceMillis = NOW - 2 * 60 * 60 * 1000L, newListsWaiting = true)),
            FollowingFeedUiState(),
        )
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun explore_phoneLandscape() = capture("explore_landscape") { CommunityHomeContent(explore, FollowingFeedUiState()) }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun explore_tablet() = capture("explore_tablet") { CommunityHomeContent(explore, FollowingFeedUiState()) }

    @Test
    @Config(qualifiers = "w700dp-h1000dp-mdpi")
    fun explore_mediumWindow() = capture("explore_medium") { CommunityHomeContent(explore, FollowingFeedUiState()) }

    @Test
    @Config(qualifiers = "w360dp-h380dp-xhdpi")
    fun explore_flipCover() = capture("explore_cover") { CommunityHomeContent(explore, FollowingFeedUiState()) }

    @Test
    fun following_light() = capture("following_light") { CommunityHomeContent(explore, following, startOnFollowing = true) }

    @Test
    fun following_dark() = capture("following_dark", dark = true) { CommunityHomeContent(explore, following, startOnFollowing = true) }

    @Test
    fun search_findsSectionsTopicsAndLists() = capture("search_everything") {
        CommunityHomeContent(
            explore.copy(
                query = "game",
                results = SearchResults(
                    lists = Popular.take(2),
                    topics = listOf(Topic("t9", "Game Boy classics", ListCategory.Games.id, TopicStatus.Approved)),
                ),
            ),
            following,
        )
    }

    @Test
    fun following_followingNothing() = capture("following_empty") {
        CommunityHomeContent(
            explore,
            FollowingFeedUiState(following = Following(emptyList(), emptyList()), feed = FeedPane(phase = FeedPhase.Ready, followingNobody = true), picked = setOf(ListCategory.Food.id)),
            startOnFollowing = true,
        )
    }

    private val games = SectionUiState(
        sectionId = ListCategory.Games.id,
        section = Section.builtIn(ListCategory.Games).copy(listCount = 812),
        countsKnown = true,
        topicsKnown = true,
        topics = listOf(
            Topic("t1", "Pokémon", ListCategory.Games.id, TopicStatus.Approved, followerCount = 210),
            Topic("t2", "Zelda", ListCategory.Games.id, TopicStatus.Approved, followerCount = 120),
            Topic("t3", "Souls-likes", ListCategory.Games.id, TopicStatus.Approved, followerCount = 90),
        ),
        waiting = listOf(Topic("w1", "Shiny hunts", ListCategory.Games.id, TopicStatus.Pending)),
        feed = FeedPane(phase = FeedPhase.Ready, lists = Popular),
        nowMillis = NOW,
    )

    @Test
    fun section_light() = capture("section_light") { SectionScreenContent(games, onBack = {}) }

    @Test
    fun section_dark() = capture("section_dark", dark = true) { SectionScreenContent(games, onBack = {}) }

    @Test
    fun section_waitsForItsTopics() = capture("section_loading") {
        SectionScreenContent(games.copy(topicsKnown = false, topics = emptyList(), waiting = emptyList()), onBack = {})
    }

    @Test
    fun section_withNothingInIt() = capture("section_empty") {
        SectionScreenContent(games.copy(topics = emptyList(), waiting = emptyList(), feed = FeedPane(phase = FeedPhase.Ready)), onBack = {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun section_phoneLandscape() = capture("section_landscape") { SectionScreenContent(games, onBack = {}) }

    private val manage = FollowingUiState.Ready(
        authors = listOf(FollowedAuthor("u-mariia", "mariia", null), FollowedAuthor("u-kyle", "kyle", null)),
        topics = listOf(Topic("t1", "Pokémon", ListCategory.Games.id, TopicStatus.Approved, followerCount = 210)),
        sections = listOf(FollowedSection(ListCategory.Food.id), FollowedSection(ListCategory.FilmTv.id)),
        waiting = listOf(Topic("w1", "Shiny hunts", ListCategory.Games.id, TopicStatus.Pending)),
        stopped = setOf(FollowKeys.person("u-kyle")),
    )

    // Past Home's twenty: every proposable sign and the nine.
    private val crowded = explore.copy(sections = Theirs + Counted)

    @Test
    fun explore_moreSectionsThanSlots_endsInAllSections() = capture("explore_more_sections") { CommunityHomeContent(crowded, FollowingFeedUiState()) }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun explore_moreSectionsThanSlots_tablet() = capture("explore_more_sections_tablet") { CommunityHomeContent(crowded, FollowingFeedUiState()) }

    @Test
    @Config(qualifiers = "w700dp-h1000dp-mdpi")
    fun explore_moreSectionsThanSlots_mediumWindow() = capture("explore_more_sections_medium") { CommunityHomeContent(crowded, FollowingFeedUiState()) }

    @Test
    fun allSections_everyIconAProposerMayPick() = capture("all_sections") {
        CompositionLocalProvider(LocalSections provides KnownSections(SectionDirectory(sections = Theirs + Counted, fetchedAtMillis = NOW))) {
            AllSectionsContent(AllSectionsUiState(sections = Theirs + Counted), onBack = {}, onProposeSection = {})
        }
    }

    @Test
    fun allSections_dark() = capture("all_sections_dark", dark = true) {
        AllSectionsContent(AllSectionsUiState(sections = Fourteen), onBack = {})
    }

    @Test
    fun manage_light() = capture("manage_light") {
        FollowingScreenContent(manage, onBack = {}, onOpenAuthor = { _, _, _ -> }, onToggle = {}, onRetry = {})
    }

    @Test
    fun manage_dark() = capture("manage_dark", dark = true) {
        FollowingScreenContent(manage, onBack = {}, onOpenAuthor = { _, _, _ -> }, onToggle = {}, onRetry = {})
    }

    private companion object {
        // Text antialiasing differs a little between the Windows that records
        // and the Linux that verifies; a real change moves far more than this.
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
