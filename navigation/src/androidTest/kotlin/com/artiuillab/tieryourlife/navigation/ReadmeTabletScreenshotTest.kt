package com.artiuillab.tieryourlife.navigation

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
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

private const val SHOT_ROOT = "readme-tablet"
private const val TAG = "ReadmeTablet"

/**
 * The tablet pictures in the README: a screen with the rail beside it.
 *
 * Drawn here and not with the phone pictures because the rail lives here.
 * `feature:tier:presentation` draws its screens without it, correctly -- a
 * screen does not know what the app puts beside it -- and for a while the
 * README showed those, stretched to tablet width, under a paragraph about a
 * rail. This is the composition `TierYourLifeNavHost` makes, with fixtures
 * where the view models would be.
 *
 * Nothing asserts. On a window without a rail it draws nothing and says so,
 * rather than writing phone-shaped pictures into a folder called tablet.
 * `docs/screenshots.md` says how to run it and where the files go.
 */
@RunWith(AndroidJUnit4::class)
class ReadmeTabletScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyTabletPictureInTheReadme() {
        var shot by mutableStateOf(SHOTS.first())
        var drawnShape: WindowShape? = null

        composeRule.setContent {
            val (screen, dark) = shot
            TierYourLifeTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    // Measured the way AppRoot measures it, so the same run on a
                    // phone knows to draw nothing rather than the wrong thing.
                    BoxWithConstraints {
                        val shape = WindowShape.of(width = maxWidth, height = maxHeight)
                        drawnShape = shape
                        CompositionLocalProvider(LocalWindowShape provides shape) {
                            // The same Row as TierYourLifeNavHost: the rail, then
                            // the content taking what is left.
                            Row(Modifier.fillMaxSize()) {
                                if (shape.hasRail) {
                                    HomeRail(selected = railItemFor(screen), onSelect = {}, onNewList = {})
                                }
                                Box(Modifier.weight(1f).fillMaxHeight()) { Screen(screen) }
                            }
                        }
                    }
                }
            }
        }

        composeRule.waitForIdle()
        if (drawnShape?.hasRail != true) {
            Log.i(TAG, "No rail at this window size ($drawnShape); nothing drawn. Run this on a tablet.")
            return
        }

        for (next in SHOTS) {
            composeRule.runOnUiThread { shot = next }
            composeRule.waitForIdle()
            // Compose is idle the moment it has asked for the artwork, not when
            // the artwork is there.
            Thread.sleep(ARTWORK_MILLIS)
            composeRule.waitForIdle()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val suffix = if (next.second) "dark" else "light"
                save("${next.first}-$suffix", composeRule.onRoot().captureToImage().asAndroidBitmap())
            }
        }
    }

    // What the real rail would light for each screen, by the same rule the
    // NavHost uses -- a board lights nothing, because the rail did not take
    // you there.
    private fun railItemFor(screen: String): RailDestination? = when (screen) {
        "board" -> railDestinationFor("Route.TierDetail/{tierListId}", onCommunity = false)
        "community" -> railDestinationFor("Route.TierLists?community={community}", onCommunity = true)
        else -> railDestinationFor("Route.Settings", onCommunity = false)
    }

    @Composable
    private fun Screen(name: String): Unit = when (name) {
        "board" -> TierDetailScreenContent(state = TierDetailUiState.Success(filmBoard()))

        "community" -> TierListsScreenContent(
            state = TierListsUiState.Success(
                lists = emptyList(),
                totalListCount = 0,
                rankedCount = 0,
                tab = HomeTab.Community,
                community = CommunityFeed.Ready(
                    listOf(
                        summary("1", "Films I make people watch", "Olena M.", ListCategory.FilmTv, 34, cover = Poster.GODFATHER),
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

        else -> SettingsScreenContent(
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
    }

    // The same invented board as the phone pictures, so the two sets are of
    // one app. Real artwork, because grey rectangles are not what it looks like.
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
                id = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", caption = "Masterpiece",
                items = listOf(TierItem(1, "The Godfather", Poster.GODFATHER), TierItem(2, "Spirited Away", Poster.SPIRITED_AWAY)),
            ),
            Tier(
                id = 2, label = "A", colorLight = "#C06A25", colorDark = "#E9A867", caption = "Great",
                items = listOf(TierItem(3, "Inception", Poster.INCEPTION), TierItem(4, "Interstellar", Poster.INTERSTELLAR)),
            ),
            Tier(
                id = 3, label = "B", colorLight = "#A98B1F", colorDark = "#D8C05A", caption = "Good",
                items = listOf(TierItem(5, "The Matrix", Poster.MATRIX)),
            ),
            Tier(id = 4, label = "C", colorLight = "#3F7F55", colorDark = "#7FC393", caption = "Watchable", items = emptyList()),
            Tier(
                id = -1, label = "Unranked", colorLight = "#DAD7E0", colorDark = "#46464F", isPool = true,
                items = listOf(TierItem(6, "Whiplash", Poster.WHIPLASH), TierItem(7, "Portrait of a 21 Year Old Woman", Poster.PORTRAIT)),
            ),
        ),
    )

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

        // Screen to whether it is drawn dark: the three the README shows.
        val SHOTS = listOf("board" to false, "community" to false, "settings" to true)
    }
}
