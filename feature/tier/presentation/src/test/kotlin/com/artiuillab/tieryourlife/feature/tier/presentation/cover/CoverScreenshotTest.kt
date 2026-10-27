package com.artiuillab.tieryourlife.feature.tier.presentation.cover

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalShortWindow
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.core.theme.ui.CoverSurface
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.SystemBarBands
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.fakeSystemBars
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val COVER_STATUS_DP = 22

/** Samsung's dots (14dp) and the navigation bar (36dp), which the cover's content stays above. */
private const val COVER_BOTTOM_DP = 50

private val Films = listOf(
    "Moonlight", "Lady Bird", "Hereditary", "Uncut Gems", "The Lighthouse", "Midsommar", "Aftersun", "Past Lives", "Ex Machina",
    "Room", "The Witch", "Minari", "Pearl", "X", "Marcel the Shell", "Everything Everywhere All at Once", "Priscilla", "Civil War",
    "Talk to Me", "Saint Maud", "Green Room", "Under the Skin", "First Reformed", "Climax",
)

private var nextId = 100L

private fun tier(id: Long, label: String, light: String, dark: String, titles: List<String>, caption: String? = null, pool: Boolean = false) =
    Tier(id, label, light, dark, titles.map { TierItem(nextId++, it, null) }, isPool = pool, caption = caption)

private val a24 = TierList(
    id = 1,
    title = "Every A24 film",
    tiers = listOf(
        tier(11, "S", "#B03A32", "#F1948C", Films.subList(0, 9), caption = "Perfect"),
        tier(12, "A", "#C06A25", "#E9A867", Films.subList(9, 13)),
        tier(13, "B", "#A98B1F", "#D8C05A", emptyList()),
        tier(14, "C", "#3F7F55", "#7FC393", Films.subList(13, 20)),
        tier(15, "D", "#3C6E99", "#86B8DE", Films.subList(20, 22)),
        tier(16, "Pool", "#DAD7E0", "#46464F", Films.subList(22, 24), pool = true),
    ),
)

private fun small(id: Long, title: String, counts: List<Int>, pool: Int = 0, author: String? = null) = TierList(
    id = id,
    title = title,
    tiers = counts.mapIndexed { i, n ->
        tier(id * 10 + i, "SABCD"[i].toString(), listOf("#B03A32", "#C06A25", "#A98B1F", "#3F7F55", "#3C6E99")[i], "#86B8DE", Films.take(n))
    } + tier(id * 10 + 9, "Pool", "#DAD7E0", "#46464F", Films.take(pool), pool = true),
    authorName = author,
)

private val ps2 = small(2, "PS2 era games", listOf(3, 5, 4, 2, 1), pool = 6)
private val lists = listOf(
    a24,
    ps2,
    small(3, "Coffee in Lviv", listOf(2, 3, 2, 1, 0)),
    small(4, "Albums I put on to work", listOf(4, 6, 8, 2, 1), pool = 3),
    small(5, "Space films that hold up", listOf(2, 3, 3, 2, 2), author = "artiman"),
)

/**
 * The cover (§4, §5, §9) at the Flip6's 352 x 339dp, with its status bar
 * above and the dots and navigation bar below drawn as bands: nothing that
 * matters sits under one. A change that moves a pixel on purpose
 * re-records them (`recordRoborazziDebug`).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w352dp-h339dp-xhdpi")
class CoverScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun show(dark: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            val window = LocalWindowInfo.current.containerSize
            val (width, height) = with(LocalDensity.current) { window.width.toDp() to window.height.toDp() }
            TierYourLifeTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalWindowShape provides WindowShape.of(width, height),
                    LocalShortWindow provides WindowShape.isShort(height),
                ) {
                    Box {
                        CoverSurface { content() }
                        SystemBarBands(COVER_STATUS_DP, COVER_BOTTOM_DP)
                    }
                }
            }
        }
        composeRule.runOnUiThread { composeRule.activity.fakeSystemBars(COVER_STATUS_DP, COVER_BOTTOM_DP) }
        composeRule.waitForIdle()
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    private fun yourLists(dark: Boolean) = show(dark) {
        CoverListsScreen(lists = lists, lastOpenedId = ps2.id, onOpen = {}, onCommunity = {}, onOpenMain = {})
    }

    private fun aList(dark: Boolean) = show(dark) {
        CoverListScreen(list = a24, next = ps2, onBack = {}, onRankOnMain = {}, onCardOnMain = { _, _ -> }, onNext = {})
    }

    @Test
    fun yourLists_light() {
        yourLists(dark = false)
        capture("cover_lists_light")
    }

    @Test
    fun yourLists_dark() {
        yourLists(dark = true)
        capture("cover_lists_dark")
    }

    @Test
    fun aListsOverview_light() {
        aList(dark = false)
        capture("cover_list_light")
    }

    @Test
    fun aListsOverview_dark() {
        aList(dark = true)
        capture("cover_list_dark")
    }

    @Test
    fun aRowOpened_light() {
        aList(dark = false)
        composeRule.onNodeWithTag(CoverTestTags.more(11)).performClick()
        capture("cover_list_row_open_light")
    }

    @Test
    fun aRowOpened_dark() {
        aList(dark = true)
        composeRule.onNodeWithTag(CoverTestTags.more(11)).performClick()
        capture("cover_list_row_open_dark")
    }

    @Test
    fun aCardPeeked_light() {
        aList(dark = false)
        composeRule.onNodeWithTag(CoverTestTags.card(a24.tiers[0].items[1].id)).performClick()
        capture("cover_list_peek_light")
    }

    @Test
    fun aCardPeeked_dark() {
        aList(dark = true)
        composeRule.onNodeWithTag(CoverTestTags.card(a24.tiers[0].items[1].id)).performClick()
        capture("cover_list_peek_dark")
    }

    @Test
    fun theEndOfAList_light() {
        aList(dark = false)
        composeRule.onNode(hasScrollToKeyAction()).performScrollToKey("end")
        capture("cover_list_end_light")
    }

    @Test
    fun theEndOfAList_dark() {
        aList(dark = true)
        composeRule.onNode(hasScrollToKeyAction()).performScrollToKey("end")
        capture("cover_list_end_dark")
    }

    @Test
    fun waitingForThePhoneToOpen_light() {
        show(dark = false) { CoverHandOffWait(CoverHandOffSubject.OwnList(a24.id, a24.title, tierId = 11), onCancel = {}) }
        capture("cover_hand_off_light")
    }

    @Test
    fun waitingForThePhoneToOpen_dark() {
        show(dark = true) { CoverHandOffWait(CoverHandOffSubject.OwnList(a24.id, a24.title, tierId = 11), onCancel = {}) }
        capture("cover_hand_off_dark")
    }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
