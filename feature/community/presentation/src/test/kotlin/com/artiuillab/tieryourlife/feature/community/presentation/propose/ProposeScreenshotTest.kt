package com.artiuillab.tieryourlife.feature.community.presentation.propose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStatus
import com.artiuillab.tieryourlife.feature.community.domain.model.ThemePair
import com.artiuillab.tieryourlife.feature.community.presentation.allsections.AllSectionsContent
import com.artiuillab.tieryourlife.feature.community.presentation.allsections.AllSectionsUiState
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

private val Counted = Section.BUILT_IN.mapIndexed { i, section ->
    section.copy(listCount = 100 + i * 37)
}

/** Waiting for a moderator, with no picture to load: the tile falls back on its tint. */
private val StreetFood = Section(
    id = "street-food",
    name = "Street food",
    icon = "local_cafe",
    tint = ThemePair("#7A4A33", "#3A2A26"),
    status = SectionStatus.Pending,
    fileUnder = ListCategory.Food.id,
    mine = true,
)

private val Kites = Section(
    id = "kites",
    name = "Kites",
    icon = "park",
    status = SectionStatus.Declined,
    declineNote = "Too close to Hobbies; try a topic there.",
    mine = true,
)

private val Bouldering = Section(id = "bouldering", name = "Bouldering", status = SectionStatus.Merged, mergedInto = ListCategory.Sport.id, mine = true)

/**
 * The proposal form and this person's own waiting section (design-spec-admin
 * §2), compared with the pictures beside the tests.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class ProposeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            val window = LocalWindowInfo.current.containerSize
            val shape = with(LocalDensity.current) { WindowShape.of(window.width.toDp(), window.height.toDp()) }
            val directory = SectionDirectory(sections = Counted + StreetFood + Kites + Bouldering, fetchedAtMillis = 1L)
            TierYourLifeTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalWindowShape provides shape, LocalSections provides KnownSections(directory)) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    private val filling = ProposeSectionUiState(
        signedIn = true,
        name = "Street food",
        nameStanding = NameStanding.Free,
        icon = "local_cafe",
        fileUnder = ListCategory.Food.id,
    )

    @Test
    fun propose_beforeAPictureIsPicked() = capture("propose_form") { ProposeSectionContent(filling, onClose = {}) }

    @Test
    fun propose_aTakenName_dark() = capture("propose_form_taken_dark", dark = true) {
        ProposeSectionContent(
            filling.copy(name = "Food", nameStanding = NameStanding.Taken(ListCategory.Food.id), pictureNotice = PictureNotice.TooSmall(720)),
            onClose = {},
        )
    }

    @Test
    fun propose_aGuest_isAskedToSignIn() = capture("propose_sign_in") {
        ProposeSectionContent(ProposeSectionUiState(signedIn = false), onClose = {})
    }

    @Test
    fun allSections_theirOwnWaitingSectionFirst_withNotices() = capture("all_sections_waiting") {
        AllSectionsContent(
            AllSectionsUiState(sections = Counted, waiting = listOf(StreetFood), notices = listOf(Kites, Bouldering)),
            onBack = {},
            onProposeSection = {},
        )
    }

    @Test
    fun allSections_theirOwnWaitingSection_dark() = capture("all_sections_waiting_dark", dark = true) {
        AllSectionsContent(AllSectionsUiState(sections = Counted, waiting = listOf(StreetFood)), onBack = {})
    }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
