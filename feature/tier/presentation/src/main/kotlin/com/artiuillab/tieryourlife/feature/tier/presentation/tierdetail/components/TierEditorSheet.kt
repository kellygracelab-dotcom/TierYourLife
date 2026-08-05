package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags

// One sheet, two modes: passing initialTier is the only difference between adding a
// new tier and editing an existing one — the fields it opens with, not a second sheet.
// The title does branch on it, though: "Edit tier" is the mock's own text and stays
// exactly as-is for edit mode, but showing that same text while adding a tier would
// mislabel the action, so add mode gets its own string.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TierEditorSheet(
    initialTier: Tier? = null,
    onDismiss: () -> Unit,
    onSave: (label: String, caption: String?, colorLight: String, colorDark: String) -> Unit,
) {
    var label by rememberSaveable { mutableStateOf(initialTier?.label ?: "") }
    var caption by rememberSaveable { mutableStateOf(initialTier?.caption ?: "") }
    var colorSelection by remember {
        mutableStateOf(
            initialTier?.let { tierColorSelectionFor(it.colorLight, it.colorDark) } ?: defaultTierColorSelection(),
        )
    }
    val isLabelValid = label.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.testTag(TierDetailTestTags.TIER_EDITOR_SHEET)) {
            Text(
                text = stringResource(
                    if (initialTier == null) R.string.tier_editor_add_title else R.string.tier_editor_title,
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(stringResource(R.string.tier_editor_label_field)) },
                        singleLine = true,
                        modifier = Modifier
                            .width(104.dp)
                            .testTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD),
                    )
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text(stringResource(R.string.tier_editor_caption_field)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD),
                    )
                }

                TierColorPicker(selection = colorSelection, onSelectionChange = { colorSelection = it })

                LivePreviewRow(
                    label = label,
                    caption = caption.ifBlank { null },
                    colorLight = colorSelection.colorLight,
                    colorDark = colorSelection.colorDark,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.tier_editor_cancel),
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag(TierDetailTestTags.TIER_EDITOR_CANCEL),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(8.dp))
                val saveContainerColor = if (isLabelValid) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                }
                val saveContentColor = if (isLabelValid) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(saveContainerColor)
                        .then(
                            if (isLabelValid) {
                                Modifier.clickable {
                                    onSave(
                                        label.trim(),
                                        caption.trim().ifBlank { null },
                                        "#${colorSelection.colorLight}",
                                        "#${colorSelection.colorDark}",
                                    )
                                }
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = 24.dp)
                        .testTag(TierDetailTestTags.TIER_EDITOR_SAVE),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.tier_editor_save),
                        color = saveContentColor,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun LivePreviewRow(label: String, caption: String?, colorLight: String, colorDark: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TierYourLifeTheme(darkTheme = false) {
            LivePreviewCard(
                themeLabel = stringResource(R.string.tier_editor_theme_light),
                label = label,
                caption = caption,
                colorLight = colorLight,
                colorDark = colorDark,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TierDetailTestTags.TIER_EDITOR_PREVIEW_LIGHT),
            )
        }
        TierYourLifeTheme(darkTheme = true) {
            LivePreviewCard(
                themeLabel = stringResource(R.string.tier_editor_theme_dark),
                label = label,
                caption = caption,
                colorLight = colorLight,
                colorDark = colorDark,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TierDetailTestTags.TIER_EDITOR_PREVIEW_DARK),
            )
        }
    }
}

@Composable
private fun LivePreviewCard(
    themeLabel: String,
    label: String,
    caption: String?,
    colorLight: String,
    colorDark: String,
    modifier: Modifier = Modifier,
) {
    val colors = tierRowColors(colorLight, colorDark)
    Column(modifier) {
        Text(
            text = themeLabel,
            style = TierYourLifeType.current.tabLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .padding(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.rowTint),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 52.dp)
                        .background(colors.band)
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = label, style = TierYourLifeType.current.tierBandLetter, color = colors.onBand)
                    if (caption != null && LocalDensity.current.fontScale < CAPTION_HIDDEN_FONT_SCALE) {
                        Text(
                            text = caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TierYourLifeType.current.tierBandCaption,
                            color = colors.onBand.copy(alpha = 0.7f),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
