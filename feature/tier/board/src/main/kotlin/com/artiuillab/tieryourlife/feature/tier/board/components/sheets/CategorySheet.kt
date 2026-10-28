package com.artiuillab.tieryourlife.feature.tier.board.components.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.layout.SheetWidth
import com.artiuillab.tieryourlife.core.theme.ui.PlusIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.common.LocalSections
import com.artiuillab.tieryourlife.feature.tier.board.common.choosableSections
import com.artiuillab.tieryourlife.feature.tier.board.common.label

/**
 * Every section a list may be filed under; [selected] and what [onSelect] gets
 * are section ids. With [withWaiting], this person's own waiting proposals
 * too, marked so: a list published into one waits under its file-under section
 * and moves once it is approved. [onPropose] ends the list with the way to
 * propose a section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySheet(
    selected: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    withWaiting: Boolean = false,
    onPropose: (() -> Unit)? = null,
) {
    AdaptiveSheet(
        onDismiss = onDismiss,
        width = SheetWidth.Choosing,
        maxHeight = 560.dp,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .selectableGroup()
                .testTag(TierDetailTestTags.CATEGORY_SHEET),
        ) {
            Text(
                text = stringResource(R.string.list_settings_category),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            choosableSections().forEach { section ->
                CategoryRow(
                    section = section,
                    selected = section.id == selected,
                    onClick = { onSelect(section.id) },
                )
            }
            if (withWaiting) {
                LocalSections.current.directory.waiting.forEach { section ->
                    CategoryRow(
                        section = section,
                        selected = section.id == selected,
                        onClick = { onSelect(section.id) },
                        waiting = true,
                    )
                }
            }
            if (onPropose != null) {
                TextButton(
                    onClick = onPropose,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag(TierDetailTestTags.PROPOSE_SECTION),
                ) {
                    PlusIcon(18.dp, MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.list_settings_propose_section), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(section: Section, selected: Boolean, onClick: () -> Unit, waiting: Boolean = false) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .testTag(TierDetailTestTags.categoryRow(section.id))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 16.dp)) {
            Text(
                text = section.label(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (waiting) {
                Text(
                    text = stringResource(R.string.list_settings_category_waiting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
