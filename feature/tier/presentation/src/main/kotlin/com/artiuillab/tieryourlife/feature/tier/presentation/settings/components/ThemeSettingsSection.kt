package com.artiuillab.tieryourlife.feature.tier.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.SettingsTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CheckIcon

private val SegmentReservedWidth = 42.dp

@Composable
internal fun ThemeSection(themeChoice: ThemeChoice, onThemeChoiceChange: (ThemeChoice) -> Unit) {
    Column(Modifier.testTag(SettingsTestTags.THEME_ROW)) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_theme_sub),
                style = TierYourLifeType.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val options = listOf(
            Triple(ThemeChoice.LIGHT, R.string.theme_light, SettingsTestTags.THEME_LIGHT),
            Triple(ThemeChoice.DARK, R.string.theme_dark, SettingsTestTags.THEME_DARK),
            Triple(ThemeChoice.SYSTEM, R.string.theme_system, SettingsTestTags.THEME_SYSTEM),
        )
        val labels = options.map { (_, labelRes, _) -> stringResource(labelRes) }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            val labelStyle = TierYourLifeType.current.supportingLabel
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val segmentWidth = maxWidth / options.size
            val widest = labels.maxOf { label ->
                with(density) {
                    measurer.measure(AnnotatedString(label), labelStyle, maxLines = 1).size.width.toDp()
                }
            }

            if (widest <= segmentWidth - SegmentReservedWidth) {
                ThemeSegmentedRow(options, labels, themeChoice, onThemeChoiceChange)
            } else {
                ThemeStackedRows(options, labels, themeChoice, onThemeChoiceChange)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSegmentedRow(
    options: List<Triple<ThemeChoice, Int, String>>,
    labels: List<String>,
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
) {
    key(LocalLayoutDirection.current) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp).height(40.dp),
        ) {
            options.forEachIndexed { index, (choice, _, tag) ->
                val selected = themeChoice == choice
                SegmentedButton(
                    selected = selected,
                    onClick = { onThemeChoiceChange(choice) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    icon = {
                        if (selected) CheckIcon(18.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                    },
                    modifier = Modifier.testTag(tag),
                ) {
                    Text(text = labels[index], style = TierYourLifeType.current.supportingLabel)
                }
            }
        }
    }
}

@Composable
private fun ThemeStackedRows(
    options: List<Triple<ThemeChoice, Int, String>>,
    labels: List<String>,
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
) {
    val outline = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier.fillMaxWidth().border(1.dp, outline, shape).clip(shape).selectableGroup(),
    ) {
        options.forEachIndexed { index, (choice, _, tag) ->
            val selected = themeChoice == choice
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(selected = selected, role = Role.RadioButton) { onThemeChoiceChange(choice) }
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag(tag),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(18.dp)) {
                    if (selected) CheckIcon(18.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = labels[index],
                    style = TierYourLifeType.current.supportingLabel,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
