package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CheckIcon

@Composable
internal fun SelectionCheckbox(isSelected: Boolean) {
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .then(
                if (isSelected) {
                    Modifier.background(primary)
                } else {
                    Modifier.border(2.dp, outline, RoundedCornerShape(4.dp))
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            CheckIcon(18.dp, onPrimary)
        }
    }
}

@Composable
internal fun SelectionBar(selectedCount: Int, fill: Color, onConfirm: () -> Unit) {
    Column(modifier = Modifier.testTag(CatalogueSearchTestTags.ITEM_SEARCH_BOTTOM_BAR)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(fill)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val countText = if (selectedCount == 0) {
                stringResource(R.string.item_search_selected_none)
            } else {
                pluralStringResource(R.plurals.item_search_selected_count, selectedCount, selectedCount)
            }
            Text(
                text = countText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(CatalogueSearchTestTags.ITEM_SEARCH_SELECTED_COUNT),
            )

            val hasSelection = selectedCount > 0
            val containerColor = if (hasSelection) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            }
            val contentColor = if (hasSelection) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
            val buttonLabel = if (hasSelection) {
                pluralStringResource(R.plurals.item_search_add_button, selectedCount, selectedCount)
            } else {
                stringResource(R.string.item_search_add_disabled)
            }
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(containerColor)
                    .then(
                        if (hasSelection) {
                            Modifier.clickable(onClick = onConfirm)
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 24.dp)
                    .testTag(CatalogueSearchTestTags.ITEM_SEARCH_CONFIRM),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = buttonLabel, style = MaterialTheme.typography.bodyMedium, color = contentColor)
            }
        }
    }
}
