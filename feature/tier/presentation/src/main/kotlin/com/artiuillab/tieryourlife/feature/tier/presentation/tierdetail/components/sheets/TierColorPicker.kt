package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.Hsl
import com.artiuillab.tieryourlife.feature.tier.presentation.common.contrastRatio
import com.artiuillab.tieryourlife.feature.tier.presentation.common.hexToHsl
import com.artiuillab.tieryourlife.feature.tier.presentation.common.parseTierColor
import com.artiuillab.tieryourlife.feature.tier.presentation.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CheckIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.ContrastIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.TuneIcon

internal data class TierPreset(val colorLight: String, val colorDark: String)

internal val TIER_COLOR_PRESETS = listOf(
    TierPreset("B03A32", "F1948C"),
    TierPreset("C06A25", "E9A867"),
    TierPreset("A98B1F", "D8C05A"),
    TierPreset("3F7F55", "8FD3A3"),
    TierPreset("3C6E99", "8FC3E8"),
    TierPreset("6B4E9E", "C9A9F0"),
    TierPreset("2F7D7D", "7ED4D4"),
    TierPreset("A63A66", "F09BB9"),
)

private const val DEFAULT_CUSTOM_LIGHT_HEX = "7A3F8C"
private const val DEFAULT_CUSTOM_DARK_HEX = "D9A2E6"

internal sealed interface TierColorSelection {
    val colorLight: String
    val colorDark: String

    data class Preset(val index: Int, override val colorLight: String, override val colorDark: String) : TierColorSelection

    data class Custom(val light: Hsl, val dark: Hsl) : TierColorSelection {
        override val colorLight: String get() = light.toHex()
        override val colorDark: String get() = dark.toHex()
    }
}

internal fun defaultTierColorSelection(): TierColorSelection = TierColorSelection.Preset(
    index = 0,
    colorLight = TIER_COLOR_PRESETS[0].colorLight,
    colorDark = TIER_COLOR_PRESETS[0].colorDark,
)

internal fun tierColorSelectionFor(colorLight: String, colorDark: String): TierColorSelection {
    val light = colorLight.removePrefix("#")
    val dark = colorDark.removePrefix("#")
    val presetIndex = TIER_COLOR_PRESETS.indexOfFirst {
        it.colorLight.equals(light, ignoreCase = true) && it.colorDark.equals(dark, ignoreCase = true)
    }
    return if (presetIndex >= 0) {
        TierColorSelection.Preset(presetIndex, TIER_COLOR_PRESETS[presetIndex].colorLight, TIER_COLOR_PRESETS[presetIndex].colorDark)
    } else {
        TierColorSelection.Custom(hexToHsl(light), hexToHsl(dark))
    }
}

private enum class ColorTab { LIGHT, DARK }

@Composable
internal fun TierColorPicker(
    selection: TierColorSelection,
    onSelectionChange: (TierColorSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.tier_editor_color_section_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        ColorSwatchesRow(
            selection = selection,
            onSelectPreset = { index ->
                val preset = TIER_COLOR_PRESETS[index]
                onSelectionChange(TierColorSelection.Preset(index, preset.colorLight, preset.colorDark))
            },
            onSelectCustom = {
                if (selection !is TierColorSelection.Custom) {
                    onSelectionChange(
                        TierColorSelection.Custom(
                            light = hexToHsl(DEFAULT_CUSTOM_LIGHT_HEX),
                            dark = hexToHsl(DEFAULT_CUSTOM_DARK_HEX),
                        ),
                    )
                }
            },
        )

        Text(
            text = stringResource(R.string.tier_editor_color_section_caption),
            style = TierYourLifeType.current.captionUnderTitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )

        val custom = selection as? TierColorSelection.Custom
        if (custom != null) {
            CustomColorPanel(
                custom = custom,
                onCustomChange = onSelectionChange,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun ColorSwatchesRow(
    selection: TierColorSelection,
    onSelectPreset: (Int) -> Unit,
    onSelectCustom: () -> Unit,
) {
    val presetDescriptionTemplate = stringResource(R.string.tier_editor_preset_color_description)
    val customDescription = stringResource(R.string.tier_editor_custom_color_description)

    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TIER_COLOR_PRESETS.forEachIndexed { index, preset ->
            ColorSwatch(
                brush = Brush.horizontalGradient(
                    listOf(parseTierColor("#${preset.colorLight}"), parseTierColor("#${preset.colorDark}")),
                ),
                isSelected = selection is TierColorSelection.Preset && selection.index == index,
                contentDescription = String.format(presetDescriptionTemplate, index + 1),
                onClick = { onSelectPreset(index) },
                modifier = Modifier.testTag(TierDetailTestTags.tierEditorPresetSwatch(index)),
            )
        }
        CustomColorSwatch(
            isSelected = selection is TierColorSelection.Custom,
            contentDescription = customDescription,
            onClick = onSelectCustom,
        )
    }
}

@Composable
private fun ColorSwatch(
    brush: Brush,
    isSelected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(if (isSelected) Modifier.border(3.dp, primary, CircleShape) else Modifier)
            .background(brush)
            .selectable(selected = isSelected, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(Modifier.size(26.dp).clip(CircleShape).background(surface), contentAlignment = Alignment.Center) {
                CheckIcon(16.dp, primary)
            }
        }
    }
}

@Composable
private fun CustomColorSwatch(isSelected: Boolean, contentDescription: String, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val conic = Brush.sweepGradient(
        listOf(
            Color(0xFFB03A32), Color(0xFFC06A25), Color(0xFFA98B1F), Color(0xFF3F7F55),
            Color(0xFF2F7D7D), Color(0xFF3C6E99), Color(0xFF6B4E9E), Color(0xFFA63A66), Color(0xFFB03A32),
        ),
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(if (isSelected) Modifier.border(3.dp, primary, CircleShape) else Modifier)
            .background(conic)
            .selectable(selected = isSelected, onClick = onClick)
            .testTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_SWATCH)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(surface), contentAlignment = Alignment.Center) {
            if (isSelected) CheckIcon(16.dp, primary) else TuneIcon(16.dp, onSurfaceVariant)
        }
    }
}

@Composable
private fun CustomColorPanel(
    custom: TierColorSelection.Custom,
    onCustomChange: (TierColorSelection.Custom) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeTab by remember { mutableStateOf(ColorTab.LIGHT) }
    val activeHsl = if (activeTab == ColorTab.LIGHT) custom.light else custom.dark
    val activeColor = activeHsl.toColor()

    fun update(newHsl: Hsl) {
        onCustomChange(if (activeTab == ColorTab.LIGHT) custom.copy(light = newHsl) else custom.copy(dark = newHsl))
    }

    Column(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Row(Modifier.fillMaxWidth()) {
            ColorTabButton(
                label = stringResource(R.string.tier_editor_theme_light),
                previewColor = custom.light.toColor(),
                isActive = activeTab == ColorTab.LIGHT,
                onClick = { activeTab = ColorTab.LIGHT },
                modifier = Modifier.weight(1f).testTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_TAB_LIGHT),
            )
            ColorTabButton(
                label = stringResource(R.string.tier_editor_theme_dark),
                previewColor = custom.dark.toColor(),
                isActive = activeTab == ColorTab.DARK,
                onClick = { activeTab = ColorTab.DARK },
                modifier = Modifier.weight(1f).testTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_TAB_DARK),
            )
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp - SLIDER_VERTICAL_COMPENSATION),
        ) {
            SliderField(
                label = stringResource(R.string.tier_editor_hue_label),
                fraction = activeHsl.hue / 360f,
                onFractionChange = { fraction -> update(activeHsl.copy(hue = fraction * 360f)) },
                track = HUE_GRADIENT,
                testTag = TierDetailTestTags.TIER_EDITOR_HUE_SLIDER,
            )
            SliderField(
                label = stringResource(R.string.tier_editor_saturation_label),
                fraction = activeHsl.saturation,
                onFractionChange = { fraction -> update(activeHsl.copy(saturation = fraction)) },
                track = Brush.horizontalGradient(listOf(Color(0xFF8B8B8B), activeColor)),
                testTag = TierDetailTestTags.TIER_EDITOR_SATURATION_SLIDER,
            )
            SliderField(
                label = stringResource(R.string.tier_editor_lightness_label),
                fraction = activeHsl.lightness,
                onFractionChange = { fraction -> update(activeHsl.copy(lightness = fraction)) },
                track = Brush.horizontalGradient(listOf(Color.Black, activeColor, Color.White)),
                testTag = TierDetailTestTags.TIER_EDITOR_LIGHTNESS_SLIDER,
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HexField(
                    hex = activeHsl.toHex(),
                    onHexCommitted = { hex -> update(hexToHsl(hex)) },
                    modifier = Modifier.weight(1f),
                )
                ContrastReadout(isDark = activeTab == ColorTab.DARK, colorLight = custom.colorLight, colorDark = custom.colorDark)
            }
        }
    }
}

private val HUE_GRADIENT = Brush.horizontalGradient(
    listOf(
        Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
        Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000),
    ),
)

@Composable
private fun ColorTabButton(
    label: String,
    previewColor: Color,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val textColor = if (isActive) primary else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isActive) primary else outlineVariant
    val borderWidthPx = if (isActive) 3.dp else 1.dp

    Row(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                val strokePx = borderWidthPx.toPx()
                drawLine(
                    color = borderColor,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height - strokePx / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height - strokePx / 2f),
                    strokeWidth = strokePx,
                )
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(previewColor)
                .border(1.dp, Color.Black.copy(alpha = 0.18f), CircleShape),
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            style = TierYourLifeType.current.tabLabel,
            color = textColor,
        )
    }
}

@Composable
private fun SliderField(
    label: String,
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    track: Brush,
    testTag: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp - SLIDER_VERTICAL_COMPENSATION),
        )
        GradientSlider(fraction = fraction, onFractionChange = onFractionChange, track = track, testTag = testTag, label = label)
    }
}

private val SLIDER_TRACK_HEIGHT = 24.dp
private val SLIDER_THUMB_WIDTH = 8.dp
private val SLIDER_THUMB_HEIGHT = 32.dp

private val SLIDER_VERTICAL_COMPENSATION = (SLIDER_THUMB_HEIGHT - SLIDER_TRACK_HEIGHT) / 2

private fun Modifier.reportZeroWidthToParent(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(0, placeable.height) {
        placeable.placeRelative(-placeable.width / 2, 0)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradientSlider(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    track: Brush,
    testTag: String,
    label: String,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Slider(
            value = fraction,
            onValueChange = onFractionChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label }
                .testTag(testTag),
            track = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(SLIDER_TRACK_HEIGHT)
                        .clip(RoundedCornerShape(12.dp))
                        .background(track)
                        .testTag(TierDetailTestTags.sliderTrack(testTag)),
                )
            },
            thumb = {
                Box(Modifier.reportZeroWidthToParent()) {
                    Box(
                        Modifier
                            .size(SLIDER_THUMB_WIDTH, SLIDER_THUMB_HEIGHT)
                            .shadow(2.dp, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                            .testTag(TierDetailTestTags.sliderThumb(testTag)),
                    )
                }
            },
        )
    }
}

@Composable
private fun HexField(hex: String, onHexCommitted: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by remember(hex) { mutableStateOf(hex) }
    OutlinedTextField(
        value = text,
        onValueChange = { newValue ->
            val filtered = newValue.uppercase().filter { it.isDigit() || it in 'A'..'F' }.take(6)
            text = filtered
            if (filtered.length == 6) onHexCommitted(filtered)
        },
        label = { Text(stringResource(R.string.tier_editor_hex_label)) },
        singleLine = true,
        modifier = modifier.testTag(TierDetailTestTags.TIER_EDITOR_HEX_FIELD),
    )
}

@Composable
private fun ContrastReadout(isDark: Boolean, colorLight: String, colorDark: String) {
    TierYourLifeTheme(darkTheme = isDark) {
        val colors = tierRowColors(colorLight, colorDark)
        val ratio = contrastRatio(colors.onBand, colors.band)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ContrastIcon(18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "%.1f:1".format(ratio),
                style = TierYourLifeType.current.captionUnderTitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TierDetailTestTags.TIER_EDITOR_CONTRAST_READOUT),
            )
        }
    }
}
