package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Sampling changes the explicit cohort, leaving the tier/pool filter intact. */
@Composable
internal fun CardSelectionOptions(picker: Picker, actions: PickerActions) {
    if (picker.cards.isEmpty()) return
    Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        val sizes = quickCounts(picker.candidates.size)
        if (sizes.isNotEmpty()) {
            Text(stringResource(R.string.play_random_sample), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sizes.forEach { count ->
                    OutlinedButton(onClick = { actions.onSample(count) }, modifier = Modifier.testTag("play_sample_$count")) {
                        Text(count.toString())
                    }
                }
            }
        }
        TextButton(onClick = actions.onShowIndividual, modifier = Modifier.testTag("play_individual_cards")) {
            Text(stringResource(R.string.play_individual_cards))
        }
    }
}

/** Only visible cards are composed, even for a board with thousands of candidates. */
@Composable
internal fun ColumnScope.IndividualCardsPage(picker: Picker, actions: PickerActions) {
    val selected = picker.selectedCards.mapTo(mutableSetOf()) { it.item.id }
    Text(stringResource(R.string.play_individual_cards), Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.headlineSmall)
    Row(Modifier.padding(horizontal = 12.dp)) {
        TextButton(onClick = { actions.onSelectCandidates(true) }) { Text(stringResource(R.string.play_select_candidates)) }
        TextButton(onClick = { actions.onSelectCandidates(false) }) { Text(stringResource(R.string.play_clear_selection)) }
    }
    LazyColumn(Modifier.weight(1f).testTag("play_individual_list")) {
        items((picker.candidates + picker.selectedCards).distinctBy { it.item.id }, key = { it.item.id }) { option ->
            val checked = option.item.id in selected
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .toggleable(checked, role = Role.Checkbox) { actions.onToggleCard(option.item.id) }
                    .testTag("play_card_${option.item.id}").padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked, onCheckedChange = null)
                Text(option.item.title.ifBlank { stringResource(R.string.play_card_unnamed) }, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                val group = picker.groups.firstOrNull { it.tierId == option.tierId }
                Text(if (group?.isPool == true) stringResource(R.string.play_pool) else group?.label.orEmpty(), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(pluralStringResource(R.plurals.play_selected, picker.count, picker.count, picker.total), Modifier.weight(1f))
        TextButton(onClick = actions.onShowCards) { Text(stringResource(R.string.play_back)) }
        TextButton(onClick = actions.onShowGames, enabled = picker.canPlay) { Text(stringResource(R.string.play_continue)) }
    }
}
