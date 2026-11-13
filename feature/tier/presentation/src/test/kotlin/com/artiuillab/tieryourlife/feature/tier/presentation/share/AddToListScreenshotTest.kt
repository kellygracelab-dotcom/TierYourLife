package com.artiuillab.tieryourlife.feature.tier.presentation.share

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkPicture
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkRefusal
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The canvas' second Android artboard, "Share a picture from the browser". */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class AddToListScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val day = 24 * 60 * 60 * 1000L
    private val lists = listOf(
        ListChoice(1, "Studio Ghibli, ranked", System.currentTimeMillis() - day - 60_000),
        ListChoice(2, "Мультфильмы детства", System.currentTimeMillis() - 3 * day - 60_000),
    )

    private fun capture(name: String, state: AddToListUiState, shared: Shared, dark: Boolean = false) {
        composeRule.setContent {
            TierYourLifeTheme(darkTheme = dark) {
                Box(Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
                    AddToListContent(state, shared, "Untitled list", onChoose = {}, onCancel = {}, onAdd = { _, _ -> })
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    @Test
    fun aPictureFromABrowser_goesIntoTheLastList() = capture(
        "add_to_list_ready",
        AddToListUiState(picture = LinkPicture.Found("https://i/1.jpg", PictureAttribution("web"), null), lists = lists, chosen = 1),
        Shared(text = "https://www.ghibli.jp/works/totoro/", subject = "Totoro"),
    )

    @Test
    fun aPageInsteadOfAPicture_saysSoAsTheSiteDoes_dark() = capture(
        "add_to_list_page_dark",
        AddToListUiState(picture = LinkPicture.Refused(LinkRefusal.NotAPicture), lists = lists, chosen = null),
        Shared(text = "https://example.com/films/totoro", subject = "Totoro"),
        dark = true,
    )

    @Test
    fun aShareIsReadFromItsIntent_aFileOrText() {
        val text = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "https://i.example.net/a.jpg")
            .putExtra(Intent.EXTRA_SUBJECT, "Totoro")
        assertEquals(Shared(text = "https://i.example.net/a.jpg", subject = "Totoro"), sharedBy(text))

        val file = Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, Uri.parse("content://browser/1"))
        assertEquals(Shared(imageUri = "content://browser/1"), sharedBy(file))

        assertNull(sharedBy(Intent(Intent.ACTION_SEND).setType("text/plain")))
        assertNull(sharedBy(Intent(Intent.ACTION_VIEW)))
    }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
