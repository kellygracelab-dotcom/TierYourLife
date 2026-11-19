package com.artiuillab.tieryourlife.feature.settings.presentation.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.settings.ColourSetting
import com.artiuillab.tieryourlife.core.theme.color.ReadyColours
import com.artiuillab.tieryourlife.core.theme.color.hueColorScheme
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.feature.settings.presentation.R
import com.artiuillab.tieryourlife.feature.settings.presentation.SettingsTestTags

/** The colours on offer, in order; the phone's only where Android has them. */
internal fun colourNames(sdk: Int = Build.VERSION.SDK_INT): List<String> = buildList {
    add(ReadyColours.LAVENDER)
    add(ReadyColours.MATCHA)
    add(ReadyColours.PEACH)
    add(ReadyColours.SKY)
    if (sdk >= Build.VERSION_CODES.S) add(ReadyColours.PHONE)
    add(ReadyColours.CUSTOM)
}

/** Which circle is chosen: a name this build does not know, or the phone's before Android 12, shows as lavender. */
internal fun shownColour(setting: ColourSetting?, sdk: Int = Build.VERSION.SDK_INT): String =
    setting?.name?.takeIf { it in colourNames(sdk) } ?: ReadyColours.LAVENDER

/**
 * Settings → Appearance → Colour (artboard 59): a tap recolours the app at once, with nothing to apply.
 * One's own colour opens [YourColourSheet].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColourSection(setting: ColourSetting?, onChoose: (name: String) -> Unit, onOwnColour: () -> Unit) {
    val shown = shownColour(setting)
    Column(Modifier.testTag(SettingsTestTags.COLOUR_ROW)) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
            Text(
                text = stringResource(R.string.settings_colour),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = colourLabel(shown),
                style = TierYourLifeType.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (name in colourNames()) {
                ColourCircle(
                    name = name,
                    ownHue = setting?.hue,
                    selected = name == shown,
                    onClick = { if (name == ReadyColours.CUSTOM) onOwnColour() else onChoose(name) },
                )
            }
        }
    }
}

@Composable
internal fun colourLabel(name: String): String = stringResource(
    when (name) {
        ReadyColours.MATCHA -> R.string.colour_matcha
        ReadyColours.PEACH -> R.string.colour_peach
        ReadyColours.SKY -> R.string.colour_sky
        ReadyColours.PHONE -> R.string.colour_phone
        ReadyColours.CUSTOM -> R.string.colour_custom
        else -> R.string.colour_lavender
    },
)

@Composable
private fun ColourCircle(name: String, ownHue: Int?, selected: Boolean, onClick: () -> Unit) {
    val label = colourLabel(name)
    val ring = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label }
            .border(if (selected) 2.dp else 1.dp, ring, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(swatchOf(name, ownHue.takeIf { selected }))
            .testTag(SettingsTestTags.colour(name)),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) CheckIcon(20.dp, Color.White)
    }
}

/** What a circle is filled with: the colour's main button; one's own, the whole wheel until it is the one chosen. */
@Composable
private fun swatchOf(name: String, ownHue: Int?): Brush {
    val hue = ReadyColours.hueOf(name)
    return when {
        hue != null -> solidBrush(hueColorScheme(hue, dark = false).primary)
        name == ReadyColours.PHONE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            solidBrush(dynamicLightColorScheme(LocalContext.current).primary)
        name == ReadyColours.CUSTOM -> ownHue?.let { solidBrush(hueColorScheme(it, dark = false).primary) } ?: HueWheel
        else -> solidBrush(LAVENDER)
    }
}

private fun solidBrush(color: Color): Brush = Brush.linearGradient(listOf(color, color))

/** Lavender's own main button, light: the app as it always was. */
private val LAVENDER = Color(0xFF4A5BAA)

internal val HueWheel: Brush = Brush.sweepGradient(
    (0..ReadyColours.FULL_TURN step 30).map { hueColorScheme(it % ReadyColours.FULL_TURN, dark = false).primary },
)
