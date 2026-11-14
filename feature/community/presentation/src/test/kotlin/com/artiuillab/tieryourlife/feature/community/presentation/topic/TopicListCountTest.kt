package com.artiuillab.tieryourlife.feature.community.presentation.topic

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A topic's page counts its lists as the section's does, from the topic's own number, as on the website. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class TopicListCountTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(listCount: Int?) {
        val topic = Topic("pixar", "Pixar", ListCategory.FilmTv.id, TopicStatus.Approved, listCount = listCount)
        composeRule.setContent {
            TierYourLifeTheme { TopicScreenContent(state = TopicUiState(name = "Pixar", section = topic.section, topic = topic), onBack = {}) }
        }
    }

    @Test
    fun theTopicsNumber_standsBesideItsLists() {
        show(listCount = 7)

        composeRule.onNodeWithText("Tier lists · 7", ignoreCase = true).assertExists()
    }

    @Test
    fun withoutANumber_theHeadingStandsAlone() {
        show(listCount = null)

        composeRule.onNodeWithText("Tier lists", ignoreCase = true).assertExists()
    }
}
