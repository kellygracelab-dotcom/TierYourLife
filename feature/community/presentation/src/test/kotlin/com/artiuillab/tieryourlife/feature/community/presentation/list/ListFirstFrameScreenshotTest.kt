package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val card = ListPreview(
    title = "Every A24 film",
    authorName = "danylo",
    itemCount = 22,
    tierColors = listOf("#B03A32", "#C06A25", "#A98B1F", "#3F7F55", "#3C6E99"),
    tierCounts = listOf(6, 7, 5, 4, 0, 0),
    authorUid = "u-danylo",
    section = "film_tv",
    rankingCount = 13,
    viewerFollowsAuthor = false,
    basedOn = BasedOn("src", "Every A24 film", "kenji"),
)

private val Films = listOf(
    "Pearl", "Moonlight", "Lady Bird", "Minari", "Aftersun", "Uncut Gems", "The Witch", "Midsommar", "Hereditary", "Ex Machina",
    "Room", "The Lighthouse", "Past Lives", "Priscilla", "X", "Talk to Me", "Mid90s", "First Cow", "Climax", "Tusk", "Saint Maud", "Waves",
)

/** The page once the list landed: the same rows, the same counts, the cards filled in where the blanks stood. */
private fun landed(): CommunityListUiState.Success {
    val first = checkNotNull(card.firstFrame("abc", signedIn = false))
    var next = 0
    val labels = listOf("S", "A", "B", "C", "D")
    val tiers = first.list.tiers.mapIndexed { index, tier ->
        tier.copy(
            label = labels.getOrElse(index) { tier.label },
            items = tier.items.map { TierItem(id = (next).toLong(), title = Films[next++], imageUrl = null) },
        )
    }
    val board = TierList(id = 0, title = card.title, tiers = tiers, authorName = "danylo")
    return first.copy(
        list = board,
        theirs = board,
        mine = board.copy(tiers = tiers.map { if (it.isPool) it.copy(items = tiers.flatMap(Tier::items)) else it.copy(items = emptyList()) }),
        arriving = false,
        follow = FollowState(following = false),
    )
}

/** The list page's first frame, before the server answered, and the page it becomes: nothing between them moves. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class ListFirstFrameScreenshotTest {

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

    @Composable
    private fun Page(state: CommunityListUiState) = CommunityListScreenContent(
        state = state,
        onBack = {},
        onMoveItem = { _, _, _ -> },
        onSave = {},
        onRetry = {},
        preview = card,
    )

    @Test
    fun firstFrame_light() = capture("list_first_frame_light") { Page(checkNotNull(card.firstFrame("abc", signedIn = false))) }

    @Test
    fun firstFrame_dark() = capture("list_first_frame_dark", dark = true) { Page(checkNotNull(card.firstFrame("abc", signedIn = false))) }

    @Test
    fun landed_light() = capture("list_landed_light") { Page(landed()) }

    @Test
    fun mine_poolOpen() = capture("list_mine_pool_open") { Page(landed().copy(showing = Showing.Mine)) }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun firstFrame_phoneLandscape() = capture("list_first_frame_landscape") { Page(checkNotNull(card.firstFrame("abc", signedIn = false))) }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
