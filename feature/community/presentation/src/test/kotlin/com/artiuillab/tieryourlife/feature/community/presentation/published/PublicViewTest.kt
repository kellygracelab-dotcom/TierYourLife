package com.artiuillab.tieryourlife.feature.community.presentation.published

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.CommunityRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class PublicViewTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun viewingThePublicSnapshot_doesNotRedirectToEditingOrSendAnUpdate() {
        val summary = PublishedListSummary(id = "pub", title = "Films", authorUid = "me", authorName = "Danylo", category = "film_tv", itemCount = 6, updatedAtMillis = 0, mine = true)
        var opened: CommunityRoute.CommunityList? = null
        var updates = 0
        compose.setContent {
            TierYourLifeTheme {
                MyPublishedScreenContent(
                    state = MyPublishedUiState.Ready(listOf(summary), canUpdate = setOf("pub")),
                    onBack = {}, onOpen = { opened = it }, onUpdate = { updates++ },
                )
            }
        }
        compose.onNodeWithTag(MyPublishedTestTags.open("pub")).performClick()
        assertNotNull(opened)
        assertTrue(opened!!.mine)
        assertTrue("the existing public-preview route bypasses the redirect to the editable board", opened!!.fromBoard)
        assertEquals(0, updates)
        compose.onNodeWithTag(MyPublishedTestTags.edit("pub")).performClick()
        assertFalse("Edit keeps the existing route to the local board", opened!!.fromBoard)
        assertEquals(0, updates)
        compose.onNodeWithTag(MyPublishedTestTags.update("pub")).performClick()
        assertEquals(1, updates)
    }
}
