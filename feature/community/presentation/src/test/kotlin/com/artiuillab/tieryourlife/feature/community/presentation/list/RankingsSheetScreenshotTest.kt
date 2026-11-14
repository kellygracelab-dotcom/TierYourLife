package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** SPEC list-flow п. 5: the author first, a thumb on everyone else's, this reader's own with Withdraw, no counts. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-port-xxhdpi")
class RankingsSheetScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val day = 1_700_000_000_000L
    private val rankings = listOf(
        Ranking("r1", "Kyle H.", null, emptyList(), emptyList(), updatedAtMillis = day, likedByMe = true),
        Ranking("me", "Ann", null, emptyList(), emptyList(), updatedAtMillis = day, mine = true),
        Ranking("r2", "Marta", null, emptyList(), emptyList(), updatedAtMillis = day),
    )

    @OptIn(ExperimentalRoborazziApi::class)
    private fun capture(name: String, dark: Boolean) {
        composeRule.setContent {
            TierYourLifeTheme(darkTheme = dark) {
                RankingsSheet(
                    state = CommunityListUiState.Success(
                        list = TierList(id = 0, title = "Films", tiers = emptyList()),
                        authorName = "Olena M.",
                        rankings = rankings,
                        rankingCount = rankings.size,
                    ),
                    onDismiss = {},
                    onChoose = {},
                    onLoadMore = {},
                    onWithdraw = {},
                )
            }
        }
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    @Test
    fun light() = capture("rankings_sheet_likes", dark = false)

    @Test
    fun dark() = capture("rankings_sheet_likes_dark", dark = true)

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
