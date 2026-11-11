package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal fun UnplayedOptions(result: ResultState, onSelect: (UnplayedPolicy) -> Unit) {
    if (result.unplayedCount == 0) return
    Column(Modifier.selectableGroup()) {
        Text(stringResource(R.string.play_unplayed_options), style = MaterialTheme.typography.labelLarge)
        UnplayedPolicy.entries.forEach { policy ->
            val selected = result.unplayedPolicy == policy
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .selectable(selected, role = Role.RadioButton) { onSelect(policy) }
                    .testTag("play_unplayed_${policy.name}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected, onClick = null)
                Text(stringResource(if (policy == UnplayedPolicy.Keep) R.string.play_unplayed_keep else R.string.play_unplayed_pool), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
