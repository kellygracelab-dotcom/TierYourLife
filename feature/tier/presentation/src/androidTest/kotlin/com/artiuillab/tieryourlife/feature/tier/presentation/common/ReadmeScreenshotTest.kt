package com.artiuillab.tieryourlife.feature.tier.presentation.common

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.settings.HiddenEntry
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.community.AuthorScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.community.AuthorUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.community.CommunityListScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.community.CommunityListUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.community.MyPublishedScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.community.MyPublishedUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.ListActionsSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.ReportDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.HiddenScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.HiddenUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.ModerationScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.ModerationUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.SettingsScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.CommunityFeed
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.HomeTab
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val SHOT_ROOT = "readme"

/**
 * The pictures in the README, drawn rather than photographed.
 *
 * They used to be screenshots of whoever was holding the phone: their boards,
 * their name, their half-finished test data. That is a poor advertisement and
 * a worse habit -- a public README is the last place a real person's name
 * should arrive by accident. These are the same screens with invented content,
 * and they can be redrawn from a command whenever the app changes, which the
 * old ones could not.
 *
 * `docs/screenshots.md` says how to run it and where the files go.
 */
@RunWith(AndroidJUnit4::class)
class ReadmeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyPictureInTheReadme() {
        val shots = SCREENS.flatMap { screen -> listOf(screen to false, screen to true) }
        var shot by mutableStateOf(shots.first())

        composeRule.setContent {
            val (screen, dark) = shot
            TierYourLifeTheme(darkTheme = dark) {
                // The whole window, so the picture is the size of the phone that
                // drew it rather than a box floating on a strip of background.
                // Which phone is written down in docs/screenshots.md.
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    Screen(screen)
                }
            }
        }

        for (next in shots) {
            composeRule.runOnUiThread { shot = next }
            composeRule.waitForIdle()
            // Compose is idle the moment it has asked for the artwork, not when
            // the artwork is there. Nothing here asserts, so waiting is cheaper
            // than a picture full of placeholders.
            Thread.sleep(ARTWORK_MILLIS)
            composeRule.waitForIdle()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val suffix = if (next.second) "dark" else "light"
                val node = if (next.first in DIALOGS) composeRule.onNode(isDialog()) else composeRule.onRoot()
                save("${next.first}-$suffix", node.captureToImage().asAndroidBitmap())
            }
        }
    }

    @Composable
    private fun Screen(name: String): Unit = when (name) {
        "home" -> TierListsScreenContent(
            state = TierListsUiState.Success(
                lists = listOf(filmBoard(), ramenBoard(), albumBoard()),
                totalListCount = 3,
                rankedCount = 2,
            ),
            onTierListClick = {},
        )

        "home-empty" -> TierListsScreenContent(
            state = TierListsUiState.Success(emptyList(), totalListCount = 0, rankedCount = 0),
            onTierListClick = {},
        )

        "board" -> TierDetailScreenContent(state = TierDetailUiState.Success(filmBoard()))

        "settings" -> SettingsScreenContent(
            account = Account.SignedIn(email = "olena@example.com", photoUrl = null, displayName = "Olena M."),
            credits = 7,
            onAccountClick = {},
            versionName = "1.0",
            themeChoice = ThemeChoice.SYSTEM,
            onThemeChoiceChange = {},
            languageTag = null,
            onLanguageTagChange = {},
            trashCount = 2,
            pendingReports = 1,
            onBack = {},
            onTrashClick = {},
            onHiddenClick = {},
            onModerationClick = {},
            onExportClick = {},
        )

        "community" -> TierListsScreenContent(
            state = TierListsUiState.Success(
                lists = emptyList(),
                totalListCount = 0,
                rankedCount = 0,
                tab = HomeTab.Community,
                community = CommunityFeed.Ready(
                    listOf(
                        summary(
                            "1", "Films I make people watch", "Olena M.", ListCategory.FilmTv, 34,
                            cover = Poster.GODFATHER,
                        ),
                        summary(
                            "3", "Every Ghibli film", "Mira", ListCategory.Anime, 22,
                            previews = listOf(Poster.SPIRITED_AWAY, Poster.INTERSTELLAR, Poster.MATRIX, Poster.INCEPTION),
                        ),
                        summary("2", "Ramen in Kyiv", "Taras", ListCategory.Food, 12),
                        summary("4", "Albums I put on to work", "Olena M.", ListCategory.Music, 41),
                    ),
                ),
            ),
            onTierListClick = {},
        )

        "community-list" -> CommunityListScreenContent(
            state = CommunityListUiState.Success(filmBoard(), "Olena M.", "u1"),
            onBack = {},
            onMoveItem = { _, _, _ -> },
            onSave = {},
            onRetry = {},
        )

        "author" -> AuthorScreenContent(
            state = AuthorUiState.Ready(
                "Olena M.",
                null,
                listOf(
                    summary(
                        "1", "Films I make people watch", "Olena M.", ListCategory.FilmTv, 34,
                        cover = Poster.GODFATHER,
                    ),
                    summary("4", "Albums I put on to work", "Olena M.", ListCategory.Music, 41),
                ),
            ),
            onBack = {},
            onOpenList = {},
            onRetry = {},
        )

        "my-published" -> MyPublishedScreenContent(
            state = MyPublishedUiState.Ready(
                listOf(
                    summary(
                        "1", "Films I make people watch", "Olena M.", ListCategory.FilmTv, 34,
                        cover = Poster.GODFATHER,
                    ),
                    summary("4", "Albums I put on to work", "Olena M.", ListCategory.Music, 41),
                ),
            ),
            onBack = {},
        )

        "hidden" -> HiddenScreenContent(
            state = HiddenUiState(
                lists = listOf(HiddenEntry("1", "Films I make people watch")),
                people = listOf(HiddenEntry("u1", "Taras")),
            ),
            onBack = {},
        )

        // Both of these are dialogs. They draw in their own window, so they are
        // captured from that rather than from the screen -- but the feed is
        // still drawn behind them, because a scrim over nothing is not what
        // anybody sees.
        "list-actions" -> {
            Screen("community")
            ListActionsSheet(
                title = "Films I make people watch",
                authorName = "Olena M.",
                authorPhotoUrl = null,
                onDismiss = {},
                onOpenAuthor = {},
                onHide = {},
                onReport = {},
            )
        }

        "report" -> {
            Screen("community")
            ReportDialog(onDismiss = {}, onSend = { _, _ -> })
        }

        else -> ModerationScreenContent(
            state = ModerationUiState.Ready(
                listOf(
                    ModerationReport(
                        listId = "1",
                        listTitle = "Films I make people watch",
                        authorName = "Olena M.",
                        reasons = listOf(ReportReason.Violence),
                        notes = listOf("The third card is a photograph of an injury."),
                        reportCount = 1,
                        newestAtMillis = 0,
                        hidden = false,
                        reviewed = false,
                    ),
                ),
            ),
            onBack = {},
        )
    }

    // Real artwork, because a tier list with grey rectangles in it is not what
    // the app looks like. TMDB serves these; if one does not arrive the tile
    // falls back to the title, so a flat network cannot fail this.
    private object Poster {
        const val GODFATHER = "https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg"
        const val SPIRITED_AWAY = "https://image.tmdb.org/t/p/w500/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg"
        const val INCEPTION = "https://image.tmdb.org/t/p/w500/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg"
        const val INTERSTELLAR = "https://image.tmdb.org/t/p/w500/yQvGrMoipbRoddT0ZR8tPoR7NfX.jpg"
        const val MATRIX = "https://image.tmdb.org/t/p/w500/dXNAPwY7VrqMAo51EKhhCJfaGb5.jpg"
        const val WHIPLASH = "https://image.tmdb.org/t/p/w500/7fn624j5lj3xTme2SgiLCeuedmO.jpg"
        const val PORTRAIT = "https://image.tmdb.org/t/p/w500/zwXutgpkOd9jdm8LhnOYrXOZvpQ.jpg"
    }

    private fun filmBoard() = TierList(
        id = 1,
        title = "Films I make people watch",
        tiers = listOf(
            Tier(
                id = 1,
                label = "S",
                colorLight = "#B03A32",
                colorDark = "#F1948C",
                items = listOf(
                    TierItem(1, "The Godfather", Poster.GODFATHER),
                    TierItem(2, "Spirited Away", Poster.SPIRITED_AWAY),
                ),
                caption = "Masterpiece",
            ),
            Tier(
                id = 2,
                label = "A",
                colorLight = "#C06A25",
                colorDark = "#E9A867",
                items = listOf(
                    TierItem(3, "Inception", Poster.INCEPTION),
                    TierItem(4, "Interstellar", Poster.INTERSTELLAR),
                ),
                caption = "Great",
            ),
            Tier(
                id = 3,
                label = "B",
                colorLight = "#A98B1F",
                colorDark = "#D8C05A",
                items = listOf(TierItem(5, "The Matrix", Poster.MATRIX)),
                caption = "Good",
            ),
            Tier(
                id = 4,
                label = "C",
                colorLight = "#3F7F55",
                colorDark = "#7FC393",
                items = emptyList(),
                caption = "Watchable",
            ),
            Tier(
                id = -1,
                label = "Unranked",
                colorLight = "#DAD7E0",
                colorDark = "#46464F",
                items = listOf(
                    TierItem(6, "Whiplash", Poster.WHIPLASH),
                    TierItem(7, "Portrait of a 21 Year Old Woman", Poster.PORTRAIT),
                ),
                isPool = true,
            ),
        ),
    )

    private fun ramenBoard() = TierList(id = 2, title = "Ramen in Kyiv", tiers = emptyList())

    private fun albumBoard() = TierList(id = 3, title = "Albums I put on to work", tiers = emptyList())

    private fun summary(
        id: String,
        title: String,
        author: String,
        category: ListCategory,
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

    private fun save(name: String, bitmap: Bitmap) {
        val outputDir = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?: InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath
        val dir = File(outputDir, SHOT_ROOT).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val ARTWORK_MILLIS = 1500L

        val DIALOGS = setOf("list-actions", "report")

        val SCREENS = listOf(
            "home", "home-empty", "board", "settings", "community", "list-actions",
            "report", "community-list", "author", "my-published", "hidden", "moderation",
        )
    }
}
