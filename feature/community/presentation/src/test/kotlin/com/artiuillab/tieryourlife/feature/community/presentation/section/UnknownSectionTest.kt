package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.tier.board.common.KnownSections
import com.artiuillab.tieryourlife.feature.tier.board.common.LocalSections
import com.artiuillab.tieryourlife.feature.tier.board.common.sectionLabel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A section id met before the phone knows it: Other's words meanwhile, one question asked, then its own name. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class UnknownSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun anUnknownId_isNamedAsOther_askedAboutOnce_andRenamedWhenFound() {
        val asked = mutableListOf<String>()
        var directory by mutableStateOf(SectionDirectory())
        composeRule.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalSections provides KnownSections(directory) { asked += it }) {
                    Text(sectionLabel("street-food"))
                }
            }
        }

        composeRule.onNodeWithText("Other").assertExists()
        composeRule.runOnIdle { assertEquals(listOf("street-food"), asked) }

        directory = SectionDirectory(extra = mapOf("street-food" to Section(id = "street-food", name = "Street food", names = mapOf("uk" to "Вулична їжа"))))

        composeRule.onNodeWithText("Street food").assertExists()
    }

    @Test
    fun anIdTheServerSaidItNeverHeardOf_staysOther_andIsNotAskedAboutAgain() {
        val asked = mutableListOf<String>()
        composeRule.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalSections provides KnownSections(SectionDirectory(unknown = setOf("ghost"))) { asked += it }) {
                    Text(sectionLabel("ghost"))
                }
            }
        }

        composeRule.onNodeWithText("Other").assertExists()
        composeRule.runOnIdle { assertEquals(emptyList<String>(), asked) }
    }

    @Test
    fun aMergedId_isNamedAsTheSectionItBecame() {
        composeRule.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalSections provides KnownSections(SectionDirectory(merged = mapOf("autos" to "food")))) {
                    Text(sectionLabel("autos"))
                }
            }
        }

        composeRule.onNodeWithText("Food").assertExists()
    }

    @Test
    fun aBuiltInTheServerHasNamed_takesTheServersWordsForThisLanguage() {
        val named = Section.BUILT_IN.map { if (it.id == "food") it.copy(name = "Food", names = mapOf("en" to "Food & drink")) else it }
        composeRule.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalSections provides KnownSections(SectionDirectory(sections = named, fetchedAtMillis = 1L))) {
                    Text(sectionLabel("food"))
                }
            }
        }

        composeRule.onNodeWithText("Food & drink").assertExists()
    }
}
