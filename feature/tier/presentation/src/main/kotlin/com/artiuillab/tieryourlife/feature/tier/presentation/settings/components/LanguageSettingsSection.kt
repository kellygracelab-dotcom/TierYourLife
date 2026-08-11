package com.artiuillab.tieryourlife.feature.tier.presentation.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.VectorIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.SettingsRow
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.SettingsTestTags

private val LanguageOptionSelectedTintLight = Color(0xFFEDEBFA)
private val LanguageOptionSelectedTintDark = Color(0xFF2E2F45)

@Composable
internal fun LanguageRow(languageTag: String?, onLanguageTagChange: (String?) -> Unit) {
    var sheetVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val current = remember(languageTag, configuration) { currentLanguageOption(context, languageTag) }

    SettingsRow(
        icon = { TranslateIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.settings_language),
        subtitle = current.nativeName,
        onClick = { sheetVisible = true },
        testTag = SettingsTestTags.LANGUAGE_ROW,
    )

    if (sheetVisible) {
        LanguageBottomSheet(
            selectedTag = languageTag,
            onSelect = { tag ->
                onLanguageTagChange(tag)
                sheetVisible = false
            },
            onDismiss = { sheetVisible = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageBottomSheet(selectedTag: String?, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.testTag(SettingsTestTags.LANGUAGE_SHEET)) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)) {
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_language_caption),
                    style = TierYourLifeType.current.captionUnderTitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.selectableGroup()) {
                LanguageOptions.forEach { option ->
                    LanguageOptionRow(
                        option = option,
                        selected = option.persistTag == selectedTag,
                        onClick = { onSelect(option.persistTag) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(option: LanguageOption, selected: Boolean, onClick: () -> Unit) {
    val isDark = TierYourLifeMedia.current.isDark
    val background = if (selected) {
        if (isDark) LanguageOptionSelectedTintDark else LanguageOptionSelectedTintLight
    } else {
        Color.Transparent
    }
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(background)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .testTag(SettingsTestTags.languageOption(option.persistTag))
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LanguageRadioDot(selected = selected, color = if (selected) accent else MaterialTheme.colorScheme.outline)
        Text(
            text = option.nativeName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(option.englishNameRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LanguageRadioDot(selected: Boolean, color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val strokeWidth = 1.6.dp.toPx()
        drawCircle(color, radius = (size.minDimension - strokeWidth) / 2f, style = Stroke(strokeWidth))
        if (selected) drawCircle(color, radius = size.minDimension / 4.5f)
    }
}

@Composable
private fun TranslateIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.5f * scale
    drawCircle(color, radius = 9f * scale, center = Offset(12f * scale, 12f * scale), style = Stroke(stroke))
    drawLine(color, Offset(3f * scale, 12f * scale), Offset(21f * scale, 12f * scale), stroke, StrokeCap.Round)
    drawArc(
        color,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(8f * scale, 3f * scale),
        size = androidx.compose.ui.geometry.Size(8f * scale, 18f * scale),
        style = Stroke(stroke),
    )
    drawArc(
        color,
        startAngle = 90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(8f * scale, 3f * scale),
        size = androidx.compose.ui.geometry.Size(8f * scale, 18f * scale),
        style = Stroke(stroke),
    )
}
