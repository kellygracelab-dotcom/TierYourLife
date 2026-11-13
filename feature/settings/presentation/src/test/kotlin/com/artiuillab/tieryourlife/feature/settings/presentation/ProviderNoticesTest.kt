package com.artiuillab.tieryourlife.feature.settings.presentation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.LinkAnnotation
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.ui.IGDB_NOTICE_TAG
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The catalogues' terms ask to be named where it stays: TMDB's notice, and IGDB.com linked beside it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class ProviderNoticesTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsName_tmdbAndIgdb_withIgdbLinkedToItsSite() {
        composeRule.setContent {
            TierYourLifeTheme {
                SettingsScreenContent(
                    account = Account.Guest,
                    credits = null,
                    onAccountClick = {},
                    versionName = "1.4.0",
                    themeChoice = ThemeChoice.SYSTEM,
                    onThemeChoiceChange = {},
                    languageTag = null,
                    onLanguageTagChange = {},
                    trashCount = 0,
                    onBack = {},
                    onTrashClick = {},
                    onHiddenClick = {},
                    onModerationClick = {},
                    onExportClick = {},
                )
            }
        }

        composeRule.onNodeWithText("This product uses the TMDB API but is not endorsed or certified by TMDB.").performScrollTo().assertExists()
        val igdb = composeRule.onNodeWithTag(IGDB_NOTICE_TAG).performScrollTo().fetchSemanticsNode()
        val text = igdb.config.getOrNull(SemanticsProperties.Text)!!.single()
        assertEquals("Game data and covers from IGDB.com", text.text)
        val link = text.getLinkAnnotations(0, text.length).single()
        assertEquals("https://www.igdb.com", (link.item as LinkAnnotation.Url).url)
        assertEquals("IGDB.com", text.text.substring(link.start, link.end))
    }
}
