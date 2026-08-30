package com.artiuillab.tieryourlife.feature.tier.presentation.common

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.text.TextUtils
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.settings.HiddenEntry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
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
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.HiddenScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.HiddenUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.ModerationScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.ModerationUiState
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
import java.util.Locale

private const val SHOT_ROOT = "screens-by-locale"
private val LOCALES = listOf("en", "ru", "uk", "de", "es", "fr", "pl", "pt-BR", "tr", "ja", "ar")

/**
 * Renders every screen in every language we ship. Failing here means a
 * translation cannot be rendered at all -- a mangled format argument, a
 * plural with no matching quantity -- which breaks a screen nobody on the
 * team reads.
 *
 * It also writes each one out. Whether long wording *fits* is a question only
 * looking answers, and long wording is the usual way a layout gives.
 */
@RunWith(AndroidJUnit4::class)
class EveryScreenInEveryLanguageTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyScreenRendersInEveryLanguage() {
        val shots = LOCALES.flatMap { locale -> SCREENS.map { locale to it } }
        var shot by mutableStateOf(shots.first())

        composeRule.setContent {
            val (languageTag, screen) = shot
            InLocale(languageTag) {
                TierYourLifeTheme(darkTheme = true) { Screen(screen) }
            }
        }

        for (next in shots) {
            composeRule.runOnUiThread { shot = next }
            composeRule.waitForIdle()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                save("${next.first}-${next.second}", composeRule.onRoot().captureToImage().asAndroidBitmap())
            }
        }
    }

    @Composable
    private fun Screen(name: String) = when (name) {
        "home-empty" -> TierListsScreenContent(
            state = TierListsUiState.Success(emptyList(), totalListCount = 0, rankedCount = 0),
            onTierListClick = {},
        )

        "home-lists" -> TierListsScreenContent(
            state = TierListsUiState.Success(
                lists = listOf(bigBoard(), smallBoard()),
                totalListCount = 2,
                rankedCount = 2,
            ),
            onTierListClick = {},
        )

        "home-community" -> TierListsScreenContent(
            state = TierListsUiState.Success(
                lists = emptyList(),
                totalListCount = 0,
                rankedCount = 0,
                tab = HomeTab.Community,
                community = CommunityFeed.Ready(listOf(summary("1"), summary("2"))),
            ),
            onTierListClick = {},
        )

        "detail" -> TierDetailScreenContent(state = TierDetailUiState.Success(bigBoard()))
        "community-list" -> CommunityListScreenContent(
            state = CommunityListUiState.Success(bigBoard(), "Olena Marchuk", "u1"),
            onBack = {},
            onMoveItem = { _, _, _ -> },
            onSave = {},
            onRetry = {},
        )

        "author" -> AuthorScreenContent(
            state = AuthorUiState.Ready("Olena Marchuk", null, listOf(summary("1"), summary("2"))),
            onBack = {},
            onOpenList = {},
            onRetry = {},
        )

        "author-empty" -> AuthorScreenContent(
            state = AuthorUiState.Ready("Olena Marchuk", null, emptyList()),
            onBack = {},
            onOpenList = {},
            onRetry = {},
        )

        "hidden" -> HiddenScreenContent(
            state = HiddenUiState(
                lists = listOf(HiddenEntry("1", "Every A24 film, ranked by how much I cried")),
                people = listOf(HiddenEntry("u1", "Olena Marchuk")),
            ),
            onBack = {},
        )

        "hidden-empty" -> HiddenScreenContent(state = HiddenUiState(), onBack = {})
        "moderation" -> ModerationScreenContent(
            state = ModerationUiState.Ready(
                listOf(
                    ModerationReport(
                        listId = "1",
                        listTitle = "Every A24 film, ranked by how much I cried",
                        authorName = "Olena Marchuk",
                        reason = ReportReason.Violence,
                        note = "The third card is a photograph of an injury.",
                        createdAtMillis = 0,
                    ),
                ),
            ),
            onBack = {},
        )

        "moderation-empty" -> ModerationScreenContent(state = ModerationUiState.Ready(emptyList()), onBack = {})
        else -> MyPublishedScreenContent(
            state = MyPublishedUiState.Ready(listOf(summary("1"), summary("2"))),
            onBack = {},
        )
    }

    @Composable
    private fun InLocale(languageTag: String, content: @Composable () -> Unit) {
        val context = LocalContext.current
        val configuration = Configuration(LocalConfiguration.current).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        val localised = context.createConfigurationContext(configuration)
        val direction = if (
            TextUtils.getLayoutDirectionFromLocale(configuration.locales[0]) == View.LAYOUT_DIRECTION_RTL
        ) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
        CompositionLocalProvider(
            LocalConfiguration provides configuration,
            LocalContext provides localised,
            LocalResources provides localised.resources,
            LocalLayoutDirection provides direction,
            content = content,
        )
    }

    private fun bigBoard() = TierList(
        id = 1,
        title = "Every A24 film, ranked by how much I cried",
        tiers = listOf(
            // Captions carry letters that hang below the line in half our
            // languages, and the band they sit in is the tightest box we draw.
            Tier(1, "S", "#B03A32", "#F1948C", listOf(TierItem(1, "Hereditary", null)), caption = "Найкращі"),
            Tier(2, "A", "#C06A25", "#E9A867", listOf(TierItem(2, "Moonlight", null)), caption = "جيد"),
            Tier(
                id = -1,
                label = "Unranked",
                colorLight = "#DAD7E0",
                colorDark = "#46464F",
                items = listOf(TierItem(3, "Lady Bird", null)),
                isPool = true,
            ),
        ),
    )

    private fun smallBoard() = TierList(id = 2, title = "Ramen", tiers = emptyList())

    private fun summary(id: String) = PublishedListSummary(
        id = id,
        title = "Every A24 film, ranked by how much I cried",
        authorUid = "u1",
        authorName = "Olena Marchuk",
        category = ListCategory.FilmTv,
        itemCount = 24,
        updatedAtMillis = 0,
    )

    private fun save(name: String, bitmap: Bitmap) {
        val outputDir = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?: InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath
        val dir = File(outputDir, SHOT_ROOT).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        val SCREENS = listOf(
            "home-empty", "home-lists", "home-community", "detail", "community-list",
            "author", "author-empty", "hidden", "hidden-empty", "moderation",
            "moderation-empty", "my-published",
        )
    }
}
