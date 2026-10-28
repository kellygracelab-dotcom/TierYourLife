package com.artiuillab.tieryourlife.feature.community.presentation.propose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.community.domain.model.MAX_SECTION_NAME
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionSymbols
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.section.SymbolIcon
import com.artiuillab.tieryourlife.feature.tier.board.common.choosableSections
import com.artiuillab.tieryourlife.feature.tier.board.common.label
import com.artiuillab.tieryourlife.feature.tier.board.common.sectionLabel

/** The name: 24 characters at most, a counter, and what the server said of it as it was typed. */
@Composable
internal fun ProposalNameField(
    name: String,
    standing: NameStanding,
    enabled: Boolean,
    onName: (String) -> Unit,
    onOpenSection: (String) -> Unit,
) {
    val full = name.length >= MAX_SECTION_NAME
    val refused = standing is NameStanding.Taken || standing == NameStanding.Bad
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ProposeTestTags.NAME),
            enabled = enabled,
            label = { Text(stringResource(R.string.propose_name)) },
            placeholder = { Text(stringResource(R.string.propose_name_placeholder)) },
            singleLine = true,
            isError = refused,
            supportingText = {
                Text(
                    "${name.length}/$MAX_SECTION_NAME",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    color = if (full) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                )
            },
        )
        when (standing) {
            is NameStanding.Taken -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.propose_name_taken, name.trim()),
                    modifier = Modifier.weight(1f, fill = false).testTag(ProposeTestTags.NAME_TAKEN),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                standing.id?.let { id ->
                    TextButton(onClick = { onOpenSection(id) }) { Text(stringResource(R.string.propose_name_open)) }
                }
            }

            NameStanding.Bad -> Text(
                stringResource(R.string.propose_name_bad),
                modifier = Modifier.testTag(ProposeTestTags.NAME_BAD),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            else -> Unit
        }
    }
}

/** The 24 icons a section may carry, six to a row; the chosen one ringed. */
@Composable
internal fun ProposalIconGrid(selected: String, enabled: Boolean, onChoose: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.propose_icon), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionSymbols.ALL.chunked(ICONS_PER_ROW).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { icon -> IconTile(icon, icon == selected, enabled, Modifier.weight(1f)) { onChoose(icon) } }
                }
            }
        }
    }
}

@Composable
private fun IconTile(icon: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(if (selected) colors.primaryContainer else colors.surfaceContainer)
            .then(if (selected) Modifier.border(2.dp, colors.primary, shape) else Modifier)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = icon.replace('_', ' ') }
            .testTag(ProposeTestTags.icon(icon)),
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(icon, 24.dp, if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant)
    }
}

/** Where the proposer's lists wait meanwhile: the open sections, built-ins first and Other last. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProposalFileUnder(selected: String, enabled: Boolean, onChoose: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val sections = choosableSections()
    Column {
        ExposedDropdownMenuBox(expanded = open, onExpandedChange = { if (enabled) open = it }) {
            OutlinedTextField(
                value = sectionLabel(selected),
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .testTag(ProposeTestTags.FILE_UNDER),
                readOnly = true,
                enabled = enabled,
                singleLine = true,
                label = { Text(stringResource(R.string.propose_file_under)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            )
            ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                sections.forEach { section ->
                    DropdownMenuItem(
                        text = { Text(section.label()) },
                        onClick = {
                            open = false
                            onChoose(section.id)
                        },
                    )
                }
            }
        }
        Text(
            stringResource(R.string.propose_file_under_hint),
            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val ICONS_PER_ROW = 6
