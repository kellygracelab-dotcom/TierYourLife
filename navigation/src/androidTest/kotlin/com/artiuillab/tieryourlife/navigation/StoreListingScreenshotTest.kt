package com.artiuillab.tieryourlife.navigation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.inspector.WindowInspector
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CommunityHomeContent
import com.artiuillab.tieryourlife.feature.community.presentation.explore.ExploreUiState
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPane
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPhase
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingFeedUiState
import com.artiuillab.tieryourlife.feature.community.presentation.list.CommunityListScreenContent
import com.artiuillab.tieryourlife.feature.community.presentation.list.CommunityListUiState
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionScreenContent
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionUiState
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.play.presentation.DuelScreen
import com.artiuillab.tieryourlife.feature.play.presentation.DuelState
import com.artiuillab.tieryourlife.feature.settings.presentation.SettingsScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.summary
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.HomeTab
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.HomeTabs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The phone pictures in the Play listing. Only pictures drawn for the app go
 * in, never a poster, a cover or a logo, because a store review reads them as
 * somebody else's work. `docs/store-listing.md` says how to run it.
 */
@RunWith(AndroidJUnit4::class)
class StoreListingScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val pictures: Map<String, String> by lazy { unpackPictures() }

    @Test
    fun everyPictureInTheListing() {
        var shot by mutableStateOf(SCREENS.first())

        composeRule.setContent {
            TierYourLifeTheme(darkTheme = shot.dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    BoxWithConstraints {
                        val shape = WindowShape.of(width = maxWidth, height = maxHeight)
                        CompositionLocalProvider(LocalWindowShape provides shape) {
                            Screen(shot.name, shot.dark)
                        }
                    }
                }
            }
        }

        SCREENS.forEachIndexed { index, next ->
            composeRule.runOnUiThread { shot = next }
            composeRule.waitForIdle()
            if (next.name == "rankings") openRankings()
            if (next.name == "drag") liftOverTheTop()
            Thread.sleep(ARTWORK_MILLIS)
            composeRule.waitForIdle()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val picture = if (next.name == "rankings") pageUnderSheet() else composeRule.onRoot().captureToImage().asAndroidBitmap()
                save("${if (next.dark) "dark" else "light"}-${index + 1}-${next.name}", picture)
            }
            if (next.name == "drag") composeRule.onNodeWithTag(TierDetailTestTags.tile(LIFTED)).performTouchInput { up() }
        }
    }

    /** A card held up over the top row on its way to a better place, the moment a board is about. */
    private fun liftOverTheTop() {
        val top = composeRule.onNodeWithTag(TierDetailTestTags.tierItems(1)).fetchSemanticsNode().boundsInRoot
        val tile = composeRule.onNodeWithTag(TierDetailTestTags.tile(LIFTED)).fetchSemanticsNode().boundsInRoot
        val to = Offset(top.left + top.width * 0.38f, top.top + top.height * 0.3f)
        val by = to - tile.center
        composeRule.onNodeWithTag(TierDetailTestTags.tile(LIFTED)).performTouchInput {
            down(center)
            advanceEventTime(HOLD_MILLIS)
            repeat(DRAG_STEPS) {
                moveBy(by / DRAG_STEPS.toFloat())
                advanceEventTime(STEP_MILLIS)
            }
        }
        composeRule.waitForIdle()
    }

    /** The way a reader gets there: the list's menu, then its Rankings line. */
    private fun openRankings() {
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Rankings ·", substring = true).onFirst().performClick()
        composeRule.waitForIdle()
    }

    /**
     * The sheet is a window of its own, which a node's picture leaves out: it
     * is drawn, scrim included, over the page's, and the status bar stays out.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun pageUnderSheet(): Bitmap {
        val page = composeRule.onNode(isRoot() and hasAnyDescendant(hasContentDescription("More options")))
            .captureToImage().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
        val sheet = WindowInspector.getGlobalWindowViews().last()
        composeRule.runOnUiThread { sheet.draw(Canvas(page)) }
        return page
    }

    @Composable
    private fun Screen(name: String, dark: Boolean): Unit = when (name) {
        "hero-board", "drag" -> TierDetailScreenContent(state = TierDetailUiState.Success(gardenBoard()))

        "board" -> TierDetailScreenContent(state = TierDetailUiState.Success(fruitBoard()))

        "settings" -> SettingsScreenContent(
            account = Account.SignedIn(email = "olena@example.com", photoUrl = null, displayName = "Olena M."),
            credits = 7,
            onAccountClick = {},
            versionName = "1.0",
            themeChoice = if (dark) ThemeChoice.DARK else ThemeChoice.LIGHT,
            onThemeChoiceChange = {},
            languageTag = null,
            onLanguageTagChange = {},
            trashCount = 2,
            pendingReports = 0,
            onBack = {},
            onTrashClick = {},
            onHiddenClick = {},
            onModerationClick = {},
            onExportClick = {},
        )

        "lists" -> TierListsScreenContent(
            state = TierListsUiState.Success(
                lists = listOf(soupBoard(), picnicBoard(), readingBoard()).map { it.summary() },
                favourites = listOf(fruitBoard().copy(favouritedAt = 1).summary()),
                grouped = true,
                totalListCount = 4,
                rankedCount = 3,
            ),
            onTierListClick = {},
            tabs = { HomeTabs(selected = HomeTab.Mine, onSelect = {}) },
        )

        "pool" -> TierDetailScreenContent(state = TierDetailUiState.Success(soupBoard()))

        "game" -> DuelScreen(
            duel = DuelState(
                kind = GameKind.Hill,
                first = item(1, "Mango", "mango"),
                second = item(2, "Strawberry", "strawberry"),
                number = 3,
                total = 10,
                canUndo = true,
            ),
            onPick = {},
            onSkip = {},
            onUndo = {},
            onQuit = {},
        )

        "section" -> SectionScreenContent(
            state = SectionUiState(
                sectionId = ListCategory.Food.id,
                section = Section.builtIn(ListCategory.Food).copy(listCount = foodLists().size),
                countsKnown = true,
                topicsKnown = true,
                topics = listOf("Fruit", "Vegetables", "Soup", "Picnic", "Berries", "Breakfast").map { name ->
                    val id = name.lowercase()
                    topic(id, name, "topic-$id")
                },
                feed = FeedPane(phase = FeedPhase.Ready, lists = foodLists()),
            ),
            onBack = {},
        )

        "rankings-a", "rankings-a-rows" -> CommunityListScreenContent(
            state = CommunityListUiState.Success(
                list = olenasFruit(withPool = !name.endsWith("-rows")),
                authorName = "Olena M.",
                authorUid = "u1",
                listId = "1",
                signedIn = true,
                rankingCount = 4,
            ),
            onBack = {},
            onMoveItem = { _, _, _ -> },
            onSave = {},
            onRetry = {},
        )

        "rankings-b", "rankings-b-rows" -> CommunityListScreenContent(
            state = CommunityListUiState.Success(
                list = olenasFruit(withPool = !name.endsWith("-rows")),
                authorName = "Olena M.",
                authorUid = "u1",
                listId = "1",
                signedIn = true,
                rankingCount = 4,
                chosenRanking = ranking("taras", "Taras", daysAgo = 1, liked = true),
                theirs = tarasFruit(withPool = !name.endsWith("-rows")),
            ),
            onBack = {},
            onMoveItem = { _, _, _ -> },
            onSave = {},
            onRetry = {},
        )

        "rankings" -> CommunityListScreenContent(
            state = CommunityListUiState.Success(
                list = fruitBoard(),
                authorName = "Olena M.",
                authorUid = "u1",
                listId = "1",
                signedIn = true,
                rankingCount = 4,
                rankings = listOf(
                    ranking("taras", "Taras", daysAgo = 1, liked = true),
                    ranking("mira", "Mira", daysAgo = 2),
                    ranking("me", "Danylo", daysAgo = 3, mine = true),
                    ranking("andrii", "Andrii K.", daysAgo = 6),
                ),
                rankingShown = true,
            ),
            onBack = {},
            onMoveItem = { _, _, _ -> },
            onSave = {},
            onRetry = {},
        )

        else -> CommunityHomeContent(
            explore = ExploreUiState(
                feed = FeedPane(
                    phase = FeedPhase.Ready,
                    lists = foodLists() + published("4", "Houseplants that survived me", "Olena M.", ListCategory.Other.id, 9),
                ),
            ),
            following = FollowingFeedUiState(),
            tabs = { HomeTabs(selected = HomeTab.Community, onSelect = {}) },
        )
    }

    private fun pic(name: String): String = pictures.getValue(name)

    private fun foodLists() = listOf(
        published("1", "Fruit, ranked honestly", "Olena M.", ListCategory.Food.id, 10, cover = pic("mango")),
        published(
            "2", "What goes in the soup", "Taras", ListCategory.Food.id, 12,
            previews = listOf(pic("tomato"), pic("carrot"), pic("broccoli"), pic("pepper")),
        ),
        published("3", "Picnic, in order of importance", "Mira", ListCategory.Food.id, 8, cover = pic("watermelon")),
    )

    private fun topic(id: String, name: String, picture: String) =
        Topic(id, name, ListCategory.Food.id, TopicStatus.Approved, pictureUrl = pic(picture))

    private fun ranking(uid: String, name: String, daysAgo: Int, liked: Boolean = false, mine: Boolean = false) = Ranking(
        uid = uid,
        authorName = name,
        authorPhotoUrl = null,
        arrangement = emptyList(),
        tierLabels = listOf("S", "A", "B", "C"),
        updatedAtMillis = System.currentTimeMillis() - daysAgo * DAY_MILLIS,
        mine = mine,
        likedByMe = liked,
    )

    private fun item(id: Long, title: String, picture: String? = null) = TierItem(id, title, picture?.let(::pic))

    private fun tier(id: Long, label: String, light: String, dark: String, caption: String, items: List<TierItem>) =
        Tier(id = id, label = label, colorLight = light, colorDark = dark, items = items, caption = caption)

    private fun pool(items: List<TierItem>) =
        Tier(id = -1, label = "Unranked", colorLight = "#DAD7E0", colorDark = "#46464F", items = items, isPool = true)

    private fun fruitBoard() = TierList(
        id = 1,
        title = "Fruit, ranked honestly",
        tiers = listOf(
            tier(1, "S", "#B03A32", "#F1948C", "Perfect", listOf(item(1, "Mango", "mango"), item(2, "Strawberry", "strawberry"))),
            tier(
                2, "A", "#C06A25", "#E9A867", "Great",
                listOf(item(3, "Cherries", "cherries"), item(4, "Peach", "peach"), item(5, "Watermelon", "watermelon")),
            ),
            tier(3, "B", "#A98B1F", "#D8C05A", "Good", listOf(item(6, "Kiwi", "kiwi"), item(7, "Orange", "orange"))),
            tier(4, "C", "#3F7F55", "#7FC393", "Fine", listOf(item(8, "Banana", "banana"))),
            pool(listOf(item(9, "Lemon", "lemon"), item(10, "Grapes", "grapes"))),
        ),
    )

    /** A new board's five tiers, every one of them full, and nothing left to place. */
    private fun gardenBoard() = TierList(
        id = 5,
        title = "Fruit & veg, ranked",
        tiers = listOf(
            tier(
                1, "S", "#B03A32", "#F1948C", "Perfect",
                listOf(
                    item(41, "Mango", "mango"), item(42, "Strawberry", "strawberry"), item(43, "Pineapple", "pineapple"),
                    item(44, "Cherries", "cherries"), item(LIFTED, "Blueberries", "blueberries"),
                ),
            ),
            tier(
                2, "A", "#C06A25", "#E9A867", "Great",
                listOf(
                    item(46, "Watermelon", "watermelon"), item(47, "Peach", "peach"), item(48, "Avocado", "avocado"),
                    item(49, "Apple", "apple"), item(50, "Grapes", "grapes"),
                ),
            ),
            tier(
                3, "B", "#A98B1F", "#D8C05A", "Good",
                listOf(
                    item(52, "Kiwi", "kiwi"), item(53, "Orange", "orange"), item(54, "Plum", "plum"),
                    item(55, "Tomato", "tomato"), item(56, "Pear", "pear"),
                ),
            ),
            tier(
                4, "C", "#3F7F55", "#7FC393", "Fine",
                listOf(
                    item(57, "Banana", "banana"), item(58, "Carrot", "carrot"), item(59, "Coconut", "coconut"),
                    item(60, "Pepper", "pepper"), item(61, "Lime", "lime"),
                ),
            ),
            tier(
                5, "D", "#3C6E99", "#86B8DE", "Meh",
                listOf(
                    item(62, "Broccoli", "broccoli"), item(63, "Lemon", "lemon"), item(64, "Pumpkin", "pumpkin"),
                    item(65, "Radish", "radish"), item(66, "Eggplant", "eggplant"),
                ),
            ),
        ),
    )

    /** Ten fruit in five full tiers, as their author placed them; an empty pool brings the bar that says whose rows these are. */
    private fun olenasFruit(withPool: Boolean) = fruitRows(
        withPool,
        listOf("Mango", "Strawberry"), listOf("Cherries", "Peach"), listOf("Watermelon", "Kiwi"),
        listOf("Banana", "Orange"), listOf("Grapes", "Lemon"),
    )

    /** The same ten as a reader placed them: mango and banana swap ends, and most move. */
    private fun tarasFruit(withPool: Boolean) = fruitRows(
        withPool,
        listOf("Banana", "Grapes"), listOf("Kiwi", "Lemon"), listOf("Strawberry", "Peach"),
        listOf("Mango", "Cherries"), listOf("Orange", "Watermelon"),
    )

    private fun fruitRows(withPool: Boolean, vararg rows: List<String>): TierList {
        val bands = listOf(
            listOf("S", "#B03A32", "#F1948C", "Perfect"), listOf("A", "#C06A25", "#E9A867", "Great"),
            listOf("B", "#A98B1F", "#D8C05A", "Good"), listOf("C", "#3F7F55", "#7FC393", "Fine"),
            listOf("D", "#3C6E99", "#86B8DE", "Meh"),
        )
        val ids = rows.flatMap { it }.sorted().withIndex().associate { (i, name) -> name to 71L + i }
        return TierList(
            id = 6,
            title = "Fruit, ranked honestly",
            tiers = rows.mapIndexed { index, names ->
                val (label, light, dark, caption) = bands[index]
                tier(index + 1L, label, light, dark, caption, names.map { item(ids.getValue(it), it, it.lowercase()) })
            } + listOfNotNull(pool(emptyList()).takeIf { withPool }),
        )
    }

    private fun soupBoard() = TierList(
        id = 2,
        title = "What goes in the soup",
        tiers = listOf(
            tier(1, "S", "#B03A32", "#F1948C", "Always", listOf(item(11, "Tomato", "tomato"))),
            tier(2, "A", "#C06A25", "#E9A867", "Usually", listOf(item(12, "Carrot", "carrot"))),
            tier(3, "B", "#A98B1F", "#D8C05A", "Maybe", emptyList()),
            tier(4, "C", "#3F7F55", "#7FC393", "Never", emptyList()),
            pool(
                listOf(
                    item(13, "Broccoli", "broccoli"), item(14, "Corn", "corn"), item(15, "Eggplant", "eggplant"),
                    item(16, "Pepper", "pepper"), item(17, "Potato"), item(18, "Onion"), item(19, "Celery"),
                    item(20, "Garlic"), item(21, "Beetroot"),
                ),
            ),
        ),
    )

    private fun picnicBoard() = TierList(
        id = 3,
        title = "Picnic, in order of importance",
        tiers = listOf(
            tier(1, "S", "#B03A32", "#F1948C", "Must", listOf(item(31, "Watermelon", "watermelon"), item(32, "Grapes", "grapes"))),
            tier(2, "A", "#C06A25", "#E9A867", "Nice", listOf(item(33, "Corn", "corn"), item(34, "Cherries", "cherries"))),
        ),
    )

    private fun readingBoard() = TierList(id = 4, title = "Books for a long train", tiers = emptyList())

    private fun published(
        id: String,
        title: String,
        author: String,
        category: String,
        itemCount: Int,
        cover: String? = null,
        previews: List<String> = emptyList(),
    ) = PublishedListSummary(
        id = id,
        title = title,
        authorUid = "u$id",
        authorName = author,
        category = category,
        itemCount = itemCount,
        coverImageUrl = cover,
        previewImages = previews,
        updatedAtMillis = 0,
    )

    private fun unpackPictures(): Map<String, String> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assets = instrumentation.context.assets
        val dir = File(instrumentation.targetContext.cacheDir, PICTURE_ROOT).apply { mkdirs() }
        return assets.list(PICTURE_ROOT).orEmpty().associate { file ->
            val target = File(dir, file)
            assets.open("$PICTURE_ROOT/$file").use { input -> target.outputStream().use(input::copyTo) }
            file.substringBefore('.') to target.absolutePath
        }
    }

    private fun save(name: String, bitmap: Bitmap) {
        val outputDir = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?: InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath
        val dir = File(outputDir, SHOT_ROOT).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val SHOT_ROOT = "listing"
        const val PICTURE_ROOT = "listing"
        const val ARTWORK_MILLIS = 800L
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        const val LIFTED = 45L
        const val HOLD_MILLIS = 600L
        const val STEP_MILLIS = 16L
        const val DRAG_STEPS = 20
        val SCREENS = listOf(
            "hero-board", "drag", "game", "rankings", "section", "rankings-a", "rankings-b", "rankings-a-rows",
            "rankings-b-rows", "board", "pool", "lists", "community", "settings",
        ).map { Shot(it, dark = false) } + listOf(
            "hero-board", "drag", "game", "section", "rankings-a", "rankings-b", "rankings-a-rows", "rankings-b-rows",
            "community", "lists",
        ).map { Shot(it, dark = true) }
    }
}

private data class Shot(val name: String, val dark: Boolean)
