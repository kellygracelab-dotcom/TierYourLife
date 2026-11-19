package com.artiuillab.tieryourlife.feature.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.color.ReadyColours
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.color.hueAppScheme
import com.artiuillab.tieryourlife.core.theme.color.vividHue
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.layout.SheetWidth
import com.artiuillab.tieryourlife.feature.settings.presentation.R
import com.artiuillab.tieryourlife.feature.settings.presentation.SettingsTestTags
import kotlin.math.roundToInt

/**
 * "Your colour" (artboard 59): one hue, and a small list drawn in it above; lightness and contrast are the
 * algorithm's. Nothing changes until Done.
 */
@Composable
internal fun YourColourSheet(startHue: Int, onDone: (hue: Int) -> Unit, onCancel: () -> Unit) {
    var hue by rememberSaveable { mutableIntStateOf(startHue.mod(ReadyColours.FULL_TURN)) }
    AdaptiveSheet(onDismiss = onCancel, width = SheetWidth.Choosing) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .testTag(SettingsTestTags.YOUR_COLOUR_SHEET),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.your_colour),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            ColourPreview(hue)
            HueSlider(hue, onHue = { hue = it })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel, modifier = Modifier.testTag(SettingsTestTags.COLOUR_CANCEL)) {
                    Text(stringResource(R.string.colour_cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onDone(hue) }, modifier = Modifier.testTag(SettingsTestTags.COLOUR_DONE)) {
                    Text(stringResource(R.string.colour_done))
                }
            }
        }
    }
}

/** A page in the colour, light or dark as the app is now: a title, three tiers and the main button. */
@Composable
private fun ColourPreview(hue: Int) {
    val dark = TierYourLifeMedia.current.isDark
    val scheme = remember(hue, dark) { hueAppScheme(hue, dark) }
    val tiers = if (dark) DARK_TIERS else LIGHT_TIERS
    Surface(
        color = scheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag(SettingsTestTags.COLOUR_PREVIEW),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.width(120.dp).height(12.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = 0.8f)))
            Spacer(Modifier.height(4.dp))
            tiers.forEachIndexed { index, band ->
                Row(
                    Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(6.dp))
                        .background(band.copy(alpha = ROW_TINT).compositeOver(scheme.surface)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(28.dp).height(28.dp).background(band), contentAlignment = Alignment.Center) {
                        Text("SAB"[index].toString(), color = if (dark) scheme.surface else Color.White, fontWeight = FontWeight.Bold)
                    }
                    repeat(index + 1) {
                        Box(Modifier.padding(start = 6.dp).size(20.dp).clip(RoundedCornerShape(4.dp)).background(scheme.surfaceContainerHighest))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.align(Alignment.End).width(96.dp).height(32.dp).clip(CircleShape).background(scheme.primary),
            )
        }
    }
}

@Composable
private fun HueSlider(hue: Int, onHue: (Int) -> Unit) {
    val label = stringResource(R.string.colour_hue)
    val thumb = remember(hue) { vividHue(hue) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(HueStrip))
        Slider(
            value = hue.toFloat(),
            onValueChange = { onHue(it.roundToInt().mod(ReadyColours.FULL_TURN)) },
            valueRange = 0f..(ReadyColours.FULL_TURN - 1).toFloat(),
            colors = SliderDefaults.colors(thumbColor = thumb, activeTrackColor = thumb.copy(alpha = 0.24f), inactiveTrackColor = thumb.copy(alpha = 0.24f)),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }.testTag(SettingsTestTags.HUE_SLIDER),
        )
    }
}

/** The rainbow under the slider: where each hue lies. */
private val HueStrip: Brush = Brush.horizontalGradient(
    (0..ReadyColours.FULL_TURN step 15).map { vividHue(it % ReadyColours.FULL_TURN) },
)

private const val ROW_TINT = 0.12f
private val LIGHT_TIERS = listOf(Color(0xFFB03A32), Color(0xFFC06A25), Color(0xFFB88A00))
private val DARK_TIERS = listOf(Color(0xFFF1948C), Color(0xFFE9A867), Color(0xFFD8C05A))
