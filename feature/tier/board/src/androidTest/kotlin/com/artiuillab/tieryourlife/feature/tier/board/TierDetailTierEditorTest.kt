package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TierDragController
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.TierRow
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TierDetailTierEditorTest : TierDetailScreenTestBase() {

    @Test
    fun addTier_withLabelAndCaption_returnsBothValuesAndBothColorHexValues() {
        var addedLabel: String? = null
        var addedCaption: String? = null
        var addedColorLight: String? = null
        var addedColorDark: String? = null
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onAddTier = { label, caption, colorLight, colorDark ->
                addedLabel = label
                addedCaption = caption
                addedColorLight = colorLight
                addedColorDark = colorDark
            },
        )

        openTierEditor()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextInput("Z")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD).performTextInput("Zenith")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle {
            assertEquals("Z", addedLabel)
            assertEquals("Zenith", addedCaption)
            assertEquals("#B03A32", addedColorLight)
            assertEquals("#F1948C", addedColorDark)
        }
    }

    @Test
    fun addTier_withEmptyCaption_returnsNullNotEmptyString() {
        var addedCaption: String? = "not yet called"
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onAddTier = { _, caption, _, _ -> addedCaption = caption },
        )

        openTierEditor()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextInput("Z")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle { assertNull(addedCaption) }
    }

    @Test
    fun selectingPreset_marksItSelectedAndItsColorsReachTheResult() {
        var addedColorLight: String? = null
        var addedColorDark: String? = null
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onAddTier = { _, _, colorLight, colorDark ->
                addedColorLight = colorLight
                addedColorDark = colorDark
            },
        )

        openTierEditor()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextInput("Z")
        composeRule.onNodeWithTag(TierDetailTestTags.tierEditorPresetSwatch(3)).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.tierEditorPresetSwatch(3)).assertIsSelected()
        composeRule.onNodeWithTag(TierDetailTestTags.tierEditorPresetSwatch(0)).assertIsNotSelected()

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle {
            assertEquals("#3F7F55", addedColorLight)
            assertEquals("#8FD3A3", addedColorDark)
        }
    }

    @Test
    fun customColor_appliesToItsOwnThemeOnly_withoutOverwritingTheOther() {
        var addedColorLight: String? = null
        var addedColorDark: String? = null
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onAddTier = { _, _, colorLight, colorDark ->
                addedColorLight = colorLight
                addedColorDark = colorDark
            },
        )

        openTierEditor()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextInput("Z")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_SWATCH).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_TAB_DARK).performScrollTo()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_TAB_DARK).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_HEX_FIELD).performScrollTo()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_HEX_FIELD).performTextClearance()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_HEX_FIELD).performTextInput("336699")

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle {
            assertEquals("#7A3F8C", addedColorLight)
            assertEquals("#336699", addedColorDark)
            assertNotEquals(addedColorLight, addedColorDark)
        }
    }

    @Test
    fun contrastWarning_doesNotBlockSavingALowContrastTier() {
        var saved = false
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onAddTier = { _, _, _, _ -> saved = true },
        )

        openTierEditor()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextInput("Z")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_SWATCH).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_HEX_FIELD).performTextClearance()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_HEX_FIELD).performTextInput("F5F0FA")

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CONTRAST_READOUT).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).assertIsDisplayed().performClick()

        composeRule.runOnIdle { assertTrue("low contrast must not block saving", saved) }
    }

    @Test
    fun draggingHueSlider_changesTheCustomColor() {
        var addedColorLight: String? = null
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onAddTier = { _, _, colorLight, _ -> addedColorLight = colorLight },
        )

        openTierEditor()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextInput("Z")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_SWATCH).performClick()

        val hueSlider = composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_HUE_SLIDER)
        hueSlider.performScrollTo()
        hueSlider.performTouchInput {
            down(Offset(width * 0.1f, center.y))
            advanceEventTime(20)
            moveTo(Offset(width * 0.9f, center.y))
            advanceEventTime(20)
            up()
        }

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle {
            assertNotEquals("#7A3F8C", addedColorLight)
        }
    }

    @Test
    fun hueSliderThumb_isVerticallyCenteredOnItsTrack() {
        setScreen(TierDetailUiState.Success(defaultList()))

        openTierEditor()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_SWATCH).performClick()

        val trackTag = TierDetailTestTags.sliderTrack(TierDetailTestTags.TIER_EDITOR_HUE_SLIDER)
        val thumbTag = TierDetailTestTags.sliderThumb(TierDetailTestTags.TIER_EDITOR_HUE_SLIDER)
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_HUE_SLIDER).performScrollTo()

        val trackBounds = composeRule.onNodeWithTag(trackTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val thumbBounds = composeRule.onNodeWithTag(thumbTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val trackCenterY = (trackBounds.top + trackBounds.bottom) / 2f
        val thumbCenterY = (thumbBounds.top + thumbBounds.bottom) / 2f

        assertEquals(
            "the thumb's vertical center must land on the track's vertical center",
            trackCenterY,
            thumbCenterY,
            0.5f,
        )
    }

    @Test
    fun doubleTapTierBand_inRankedTier_opensEditorPrefilledWithItsValues() {
        val list = listOf(
            tier(
                id = 1,
                label = "S",
                items = emptyList(),
                caption = "Masterpiece",
                colorLight = "#111111",
                colorDark = "#222222",
            ),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { doubleClick() }

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SHEET).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).assertTextContains("S")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD).assertTextContains("Masterpiece")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_SWATCH).assertIsSelected()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_PREVIEW_LIGHT).performScrollTo()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_PREVIEW_LIGHT).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_PREVIEW_DARK).assertIsDisplayed()
    }

    @Test
    fun doubleTapTierBand_onThePool_doesNotOpenEditor() {
        var editedId: Long? = null
        val pool = tier(id = 6, label = "Pool", items = emptyList(), isPool = true)

        composeRule.setContent {
            TierYourLifeTheme {
                val dragController = remember { TierDragController() }
                TierRow(
                    tier = pool,
                    bandContentWidth = 64.dp,
                    displayMode = TierListDisplayMode.WRAP,
                    dragController = dragController,
                    rankedTierIds = emptyList(),
                    onMoveItem = { _, _, _ -> },
                    onDeleteItem = {},
                    onReorderTiers = {},
                    onDeleteTier = {},
                    onEditTier = { id -> editedId = id },
                )
            }
        }

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(6)).performTouchInput { doubleClick() }

        composeRule.runOnIdle { assertNull(editedId) }
    }

    @Test
    fun editingTier_savesNewLabelAndCaptionExactlyOnce() {
        var callCount = 0
        var editedLabel: String? = null
        var editedCaption: String? = null
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList(), caption = "Old caption"),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(
            TierDetailUiState.Success(list),
            onEditTier = { _, label, caption, _, _ ->
                callCount++
                editedLabel = label
                editedCaption = caption
            },
        )

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextClearance()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextInput("S+")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD).performTextClearance()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD).performTextInput("New caption")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle {
            assertEquals(1, callCount)
            assertEquals("S+", editedLabel)
            assertEquals("New caption", editedCaption)
        }
    }

    @Test
    fun editingTier_savesBothColorValues() {
        var editedColorLight: String? = null
        var editedColorDark: String? = null
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(
            TierDetailUiState.Success(list),
            onEditTier = { _, _, _, colorLight, colorDark ->
                editedColorLight = colorLight
                editedColorDark = colorDark
            },
        )

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.tierEditorPresetSwatch(3)).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle {
            assertEquals("#3F7F55", editedColorLight)
            assertEquals("#8FD3A3", editedColorDark)
        }
    }

    @Test
    fun editingTier_withEmptyCaption_savesNullNotEmptyString() {
        var editedCaption: String? = "not yet called"
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList(), caption = "Has a caption"),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(
            TierDetailUiState.Success(list),
            onEditTier = { _, _, caption, _, _ -> editedCaption = caption },
        )

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD).performTextClearance()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle { assertNull(editedCaption) }
    }

    @Test
    fun editingTier_withEmptyLabel_doesNotSave() {
        var callCount = 0
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(
            TierDetailUiState.Success(list),
            onEditTier = { _, _, _, _, _ -> callCount++ },
        )

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextClearance()

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).assertHasNoClickAction()
        composeRule.runOnIdle { assertEquals(0, callCount) }
    }
}
