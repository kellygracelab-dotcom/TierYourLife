package com.artiuillab.tieryourlife.navigation

import android.graphics.Bitmap
import android.os.Build
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CommunityHomeContent
import com.artiuillab.tieryourlife.feature.community.presentation.explore.ExploreUiState
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPane
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPhase
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingFeedUiState
import com.artiuillab.tieryourlife.feature.settings.presentation.SettingsScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
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
            TierYourLifeTheme(darkTheme = true) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    BoxWithConstraints {
                        val shape = WindowShape.of(width = maxWidth, height = maxHeight)
                        CompositionLocalProvider(LocalWindowShape provides shape) {
                            Screen(shot)
                        }
                    }
                }
            }
        }

        SCREENS.forEachIndexed { index, next ->
            composeRule.runOnUiThread { shot = next }
            composeRule.waitForIdle()
            Thread.sleep(ARTWORK_MILLIS)
            composeRule.waitForIdle()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                save("${index + 1}-$next", composeRule.onRoot().captureToImage().asAndroidBitmap())
            }
        }
    }

    @Composable
    private fun Screen(name: String): Unit = when (name) {
        "board" -> TierDetailScreenContent(state = TierDetailUiState.Success(fruitBoard()))

        "settings" -> SettingsScreenContent(
            account = Account.SignedIn(email = "olena@example.com", photoUrl = null, displayName = "Olena M."),
            credits = 7,
            onAccountClick = {},
            versionName = "1.0",
            themeChoice = ThemeChoice.DARK,
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

        else -> CommunityHomeContent(
            explore = ExploreUiState(
                feed = FeedPane(
                    phase = FeedPhase.Ready,
                    lists = listOf(
                        published("1", "Fruit, ranked honestly", "Olena M.", ListCategory.Food.id, 10, cover = pic("mango")),
                        published(
                            "2", "What goes in the soup", "Taras", ListCategory.Food.id, 12,
                            previews = listOf(pic("tomato"), pic("carrot"), pic("broccoli"), pic("pepper")),
                        ),
                        published("3", "Picnic, in order of importance", "Mira", ListCategory.Food.id, 8, cover = pic("watermelon")),
                        published("4", "Houseplants that survived me", "Olena M.", ListCategory.Other.id, 9),
                    ),
                ),
            ),
            following = FollowingFeedUiState(),
            tabs = { HomeTabs(selected = HomeTab.Community, onSelect = {}) },
        )
    }

    private fun pic(name: String): String = pictures.getValue(name)

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
        val SCREENS = listOf("board", "settings", "lists", "pool", "community")
    }
}
