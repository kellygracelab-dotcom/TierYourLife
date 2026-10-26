package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalShortWindow
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.previewTierLists
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.components.previewTrashEntries
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Your lists (and the trash beside it) in the windows Danylo found broken:
 * sideways, where one bar with the counts in it leaves with the boards, and
 * on tablets, where the bar and the boards stand on one capped column.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class TierListsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            val window = LocalWindowInfo.current.containerSize
            val (width, height) = with(LocalDensity.current) { window.width.toDp() to window.height.toDp() }
            TierYourLifeTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalWindowShape provides WindowShape.of(width, height),
                    LocalShortWindow provides WindowShape.isShort(height),
                ) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    private val boards = TierListsUiState.Success(previewTierLists, previewTierLists.size, 24)

    @Test
    fun lists_phonePortrait() = capture("lists_phone_portrait") { TierListsScreenContent(boards, onTierListClick = {}) }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun lists_phoneLandscape_rows() = capture("lists_landscape_rows") { TierListsScreenContent(boards, onTierListClick = {}) }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun lists_phoneLandscape_rows_dark() = capture("lists_landscape_rows_dark", dark = true) {
        TierListsScreenContent(boards, onTierListClick = {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun lists_phoneLandscape_grid() = capture("lists_landscape_grid") {
        TierListsScreenContent(boards.copy(asPictures = true), onTierListClick = {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun lists_phoneLandscape_grid_dark() = capture("lists_landscape_grid_dark", dark = true) {
        TierListsScreenContent(boards.copy(asPictures = true), onTierListClick = {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun lists_phoneLandscape_search() = capture("lists_landscape_search") {
        TierListsScreenContent(
            boards.copy(lists = previewTierLists.take(2), mode = HomeMode.Searching("fi")),
            onTierListClick = {},
        )
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-mdpi")
    fun lists_tabletPortrait() = capture("lists_tablet_portrait") { TierListsScreenContent(boards, onTierListClick = {}) }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-mdpi")
    fun lists_tabletPortrait_grid() = capture("lists_tablet_portrait_grid") {
        TierListsScreenContent(boards.copy(asPictures = true), onTierListClick = {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun trash_phoneLandscape() = capture("trash_landscape") {
        TrashScreenContent(state = TrashUiState.Success(previewTrashEntries), onBack = {})
    }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
