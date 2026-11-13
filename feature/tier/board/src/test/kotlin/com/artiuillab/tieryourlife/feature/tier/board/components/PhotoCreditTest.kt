package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.MoveItemSheetContent
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.sourceNameOf
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A card's menu says who made its picture and where it is from (13.11 canvas, "Who made this picture"). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class PhotoCreditTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun open(item: TierItem) {
        val ranked = previewTierList.tiers.filterNot { it.isPool }
        composeRule.setContent {
            TierYourLifeTheme {
                MoveItemSheetContent(
                    item = item,
                    currentTierId = ranked.first().id,
                    rankedTiers = ranked,
                    pool = previewTierList.tiers.first { it.isPool },
                    onMoveToTier = { _, _ -> },
                    onRemove = {},
                )
            }
        }
    }

    private fun about() = composeRule.onNodeWithTag(TierDetailTestTags.MOVE_SHEET_ABOUT_PICTURE)

    @Test
    fun anAttributedPicture_namesItsAuthorLicenceAndSource() {
        val fox = PictureAttribution(
            source = "inaturalist",
            author = "Jane Doe",
            license = "CC BY 4.0",
            licenseUrl = "https://creativecommons.org/licenses/by/4.0/",
            link = "https://www.inaturalist.org/photos/1",
        )
        open(TierItem(id = 1, title = "Red fox", imageUrl = "https://example.org/fox.jpg", attribution = fox))

        about().performClick()

        listOf("Author", "Jane Doe", "License", "CC BY 4.0", "Source", "iNaturalist").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
        composeRule.onNodeWithText("This product uses the TMDB API", substring = true).assertDoesNotExist()
    }

    @Test
    fun whatIsNotKnown_isNotShown() {
        open(TierItem(id = 1, title = "Alien", imageUrl = "https://example.org/a.jpg", attribution = PictureAttribution("web")))

        about().performClick()

        listOf("Author", "License", "Source").forEach { composeRule.onNodeWithText(it).assertDoesNotExist() }
    }

    @Test
    fun aTmdbPicture_carriesTmdbsNotice_byItsAttributionNotItsAddress() {
        val film = PictureAttribution("tmdb", link = "https://www.themoviedb.org/movie/348")
        open(TierItem(id = 1, title = "Alien", imageUrl = "https://firebasestorage.googleapis.com/a.jpg", attribution = film))

        about().performClick()

        composeRule.onNodeWithText("TMDB").assertExists()
        composeRule.onNodeWithText("This product uses the TMDB API but is not endorsed or certified by TMDB.").assertExists()
    }

    @Test
    fun aCardPickedBeforeAttributions_keepsItsCredit() {
        open(TierItem(id = 1, title = "Red fox", imageUrl = "https://example.org/fox.jpg", credit = "© Jane Doe, CC BY"))

        about().performClick()

        composeRule.onNodeWithText("© Jane Doe, CC BY").assertExists()
    }

    @Test
    fun someonesOwnPhoto_andACardWithoutAPicture_sayNothing() {
        open(TierItem(id = 1, title = "Arrival", imageUrl = "/data/pictures/own.jpg"))

        about().assertDoesNotExist()
    }

    @Test
    fun aSiteIsNamedByItsPage_andACatalogueByItsName() {
        assertEquals("ghibli.jp", sourceNameOf(PictureAttribution("web", link = "https://www.ghibli.jp/works/totoro/")))
        assertEquals("Wikimedia Commons", sourceNameOf(PictureAttribution("wikimedia")))
        assertNull(sourceNameOf(PictureAttribution("web")))
    }
}
