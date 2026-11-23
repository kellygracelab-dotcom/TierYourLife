package com.artiuillab.tieryourlife.feature.tier.board.share

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A published board's link, one tap from its bar: what it says and when it is there. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ShareLinkTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val placed = previewTierList.copy(tiers = previewTierList.tiers.map { if (it.isPool) it.copy(items = emptyList()) else it })

    private fun tier(label: String, vararg names: String, pool: Boolean = false) = Tier(
        id = label.hashCode().toLong(),
        label = label,
        colorLight = "#111111",
        colorDark = "#EEEEEE",
        items = names.mapIndexed { index, name -> TierItem(id = index.toLong(), title = name, imageUrl = null) },
        isPool = pool,
    )

    private fun list(vararg tiers: Tier) = TierList(id = 1, title = "Games", tiers = tiers.toList())

    @Test
    fun theHook_namesTheTopTiersFirstThree() {
        val hook = shareHookOf(list(tier("S", "Hades", "Celeste", "Inside", "Limbo"), tier("A", "Braid")))
        assertEquals(ShareHook("S", listOf("Hades", "Celeste", "Inside")), hook)
    }

    @Test
    fun anEmptyTopTier_givesWayToTheFirstRowWithCards() {
        val hook = shareHookOf(list(tier("S"), tier("A", "Braid", "Fez")))
        assertEquals(ShareHook("A", listOf("Braid", "Fez")), hook)
    }

    // A picture without a name says nothing in a message.
    @Test
    fun aTopTierOfPicturesOnly_fallsBackToTheFirstNamedCards() {
        val hook = shareHookOf(list(tier("S", "", " "), tier("A", "Braid"), tier("B", "Fez")))
        assertEquals(ShareHook(null, listOf("Braid", "Fez")), hook)
    }

    @Test
    fun theUnplaced_andNamelessBoards_haveNoHook() {
        assertNull(shareHookOf(list(tier("S"), tier("", "Hades", pool = true))))
        assertNull(shareHookOf(list(tier("S", ""))))
    }

    @Test
    fun theText_isTheHookAndTheLink_orTheTitleAndTheLink() {
        val resources = ApplicationProvider.getApplicationContext<Application>().resources
        assertEquals(
            "My S tier: Hades, Celeste. What’s yours?\nhttps://tieryourlife.com/l/x",
            shareLinkText(resources, list(tier("S", "Hades", "Celeste")), "https://tieryourlife.com/l/x"),
        )
        assertEquals("Games\nhttps://tieryourlife.com/l/x", shareLinkText(resources, list(tier("S", "")), "https://tieryourlife.com/l/x"))
    }

    @Test
    fun aPublishedBoard_sharesItsLinkFromTheBar() {
        show(placed.copy(publishedId = "pub-1"))

        composeRule.onNodeWithTag(TierDetailTestTags.SHARE_LINK).performClick()

        val chooser = shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity

        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals(
            "My S tier: Interstellar, Arrival. What’s yours?\nhttps://tieryourlife.com/l/pub-1",
            send.getStringExtra(Intent.EXTRA_TEXT),
        )
        capture("board_share_link")
    }

    @Test
    fun aChangedOne_offersUpdateAndTheLinkBoth() {
        show(placed.copy(publishedId = "pub-1"), behind = true)

        composeRule.onNodeWithTag(TierDetailTestTags.PUBLISH).assertExists()
        composeRule.onNodeWithTag(TierDetailTestTags.SHARE_LINK).assertExists()
        capture("board_share_link_behind")
    }

    @Test
    fun aBoardNotOut_hasNoLink() {
        show(placed)

        assertEquals(0, composeRule.onAllNodes(hasTestTag(TierDetailTestTags.SHARE_LINK)).fetchSemanticsNodes().size)
    }

    @Test
    fun someoneElsesList_hasNoLinkOfTheirs() {
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(placed.copy(publishedId = "pub-1")),
                    actions = TierDetailActions(onPublish = {}),
                    readOnly = true,
                )
            }
        }

        assertEquals(0, composeRule.onAllNodes(hasTestTag(TierDetailTestTags.SHARE_LINK)).fetchSemanticsNodes().size)
    }

    private fun show(list: TierList, behind: Boolean = false) {
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(list),
                    actions = TierDetailActions(onPlay = {}, publishedIsBehind = behind, onPublish = {}),
                )
            }
        }
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            "src/test/screenshots/$name.png",
            roborazziOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F)),
        )
    }
}
