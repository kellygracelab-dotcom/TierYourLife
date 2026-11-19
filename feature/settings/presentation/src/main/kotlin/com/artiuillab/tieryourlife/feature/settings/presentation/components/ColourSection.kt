package com.artiuillab.tieryourlife.feature.settings.presentation.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.settings.ColourChoice
import com.artiuillab.tieryourlife.core.settings.ColourSetting
import com.artiuillab.tieryourlife.core.theme.color.ReadyColours
import com.artiuillab.tieryourlife.core.theme.color.swatchScheme
import com.artiuillab.tieryourlife.core.theme.color.vividHue
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.core.theme.ui.PencilIcon
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
        // One row, as on artboard 59: the circles share the width, 40–44 dp on a phone.
        BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
            val names = colourNames()
            val circle = ((maxWidth - MIN_GAP * (names.size - 1)) / names.size).coerceIn(MIN_CIRCLE, MAX_CIRCLE)
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                for (name in names) {
                    ColourCircle(
                        name = name,
                        ownHue = setting?.hue,
                        selected = name == shown,
                        size = circle,
                        onClick = { if (name == ReadyColours.CUSTOM) onOwnColour() else onChoose(name) },
                    )
                }
            }
        }
    }
}

/** "Your colour" opens on the hue the app is in now: a ready colour's own, else the last own one. */
internal fun startHueOf(setting: ColourSetting?): Int =
    setting?.let { ReadyColours.hueOf(it.name) ?: it.hue } ?: ColourChoice.LAVENDER_HUE

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

/**
 * A colour as its light page shows it, in light and dark alike: the page's light tone with its main colour
 * as a dot. One's own has a pencil, on the rainbow until it is the one chosen, then on its own colour.
 */
@Composable
private fun ColourCircle(name: String, ownHue: Int?, selected: Boolean, size: Dp, onClick: () -> Unit) {
    val label = colourLabel(name)
    val ring = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
    val own = name == ReadyColours.CUSTOM
    val context = LocalContext.current
    val colour = if (own) ownHue?.takeIf { selected }?.let { ReadyColours.colourOf(name, it) } else ReadyColours.colourOf(name, 0)
    val swatch = colour?.let { remember(it) { swatchScheme(it, context) } }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label }
            .border(if (selected) 2.dp else 1.dp, ring, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(swatch?.let { Brush.linearGradient(listOf(it.primaryContainer, it.primaryContainer)) } ?: HueWheel)
            .testTag(SettingsTestTags.colour(name)),
        contentAlignment = Alignment.Center,
    ) {
        if (swatch == null) {
            Mark(MaterialTheme.colorScheme.surface) { PencilIcon(16.dp, MaterialTheme.colorScheme.onSurface) }
        } else {
            Mark(swatch.primary, large = selected || own) {
                if (own) PencilIcon(16.dp, swatch.onPrimary) else if (selected) CheckIcon(16.dp, swatch.onPrimary)
            }
        }
    }
}

/** The dot in a circle: the colour's main one, larger with a check or one's own pencil. */
@Composable
private fun Mark(color: Color, large: Boolean = true, content: @Composable () -> Unit) {
    Box(
        Modifier.size(if (large) 24.dp else 16.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private val MIN_GAP = 4.dp
private val MIN_CIRCLE = 36.dp
private val MAX_CIRCLE = 44.dp

/** Every hue as vivid as the screen shows it, around the circle. */
internal val HueWheel: Brush = Brush.sweepGradient(
    (0..ReadyColours.FULL_TURN step 15).map { vividHue(it % ReadyColours.FULL_TURN) },
)
