package com.artiuillab.tieryourlife.feature.tier.presentation.community

import android.content.res.Configuration
import android.graphics.Bitmap
import android.text.TextUtils
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.AuthorActionsSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.ListActionsSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.ReportDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

private const val SHOT_ROOT = "reporting-locales"
private val LOCALES = listOf("en", "ru", "uk", "de", "es", "fr", "pl", "pt-BR", "tr", "ja", "ar")
private val SURFACES = listOf("report", "list-actions", "author-actions")

/**
 * Renders every reporting surface in every language we ship. Failing here
 * means a translation cannot be rendered at all -- a mangled format argument,
 * an unescaped apostrophe -- which is a real and recurring way to break a
 * screen nobody on the team reads.
 *
 * It also writes each one out, because whether long wording *fits* is a
 * question only looking answers. Mirroring is not among them: a bottom sheet
 * lives in its own window and takes its direction from the activity, which
 * this harness cannot swap, so RTL has to be judged on a device set to it.
 */
@RunWith(AndroidJUnit4::class)
class ReportingInEveryLanguageTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everySurfaceRendersInEveryLanguage() {
        val shots = LOCALES.flatMap { locale -> SURFACES.map { locale to it } }
        var shot by mutableStateOf(shots.first())

        composeRule.setContent {
            val (languageTag, surface) = shot
            InLocale(languageTag) {
                TierYourLifeTheme(darkTheme = true) { Surface(surface) }
            }
        }

        for (next in shots) {
            composeRule.runOnUiThread { shot = next }
            composeRule.waitForIdle()
            save(
                name = "${next.first}-${next.second}",
                bitmap = composeRule.onNode(isDialog()).captureToImage().asAndroidBitmap(),
            )
        }
    }

    @Composable
    private fun Surface(kind: String) = when (kind) {
        "report" -> ReportDialog(onDismiss = {}, onSend = { _, _ -> })
        "list-actions" -> ListActionsSheet(
            title = "Every A24 film, ranked by how much I cried",
            authorName = "Danylo Kovalenko",
            authorPhotoUrl = null,
            onDismiss = {},
            onOpenAuthor = {},
            onHide = {},
            onReport = {},
        )

        else -> AuthorActionsSheet(
            name = "Danylo Kovalenko",
            photoUrl = null,
            onDismiss = {},
            onHideAuthor = {},
        )
    }

    @Composable
    private fun InLocale(languageTag: String, content: @Composable () -> Unit) {
        val context = LocalContext.current
        val configuration = Configuration(LocalConfiguration.current).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        val localised = context.createConfigurationContext(configuration)
        // The real app takes its direction from the activity's configuration;
        // a swapped-in one here has to say so itself.
        val direction = if (TextUtils.getLayoutDirectionFromLocale(configuration.locales[0]) ==
            View.LAYOUT_DIRECTION_RTL
        ) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
        CompositionLocalProvider(
            LocalConfiguration provides configuration,
            LocalContext provides localised,
            LocalResources provides localised.resources,
            LocalLayoutDirection provides direction,
            content = content,
        )
    }

    // Gradle pulls whatever lands in additionalTestOutputDir off the device
    // after the run, which beats guessing at a path on external storage.
    private fun save(name: String, bitmap: Bitmap) {
        val outputDir = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?: InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath
        val dir = File(outputDir, SHOT_ROOT).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
