package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.account.presentation.R
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountTestTags
import com.artiuillab.tieryourlife.feature.account.presentation.account.GuestBoardsQuestion

private val BUTTON_HEIGHT = 52.dp

/**
 * Asked on signing into an account used before, only when this phone has
 * boards made without an account. Only those: another account's boards are
 * offered only once its session was ended for it, in a question of their own.
 * Neither answer deletes anything.
 */
@Composable
internal fun MergePicker(
    question: GuestBoardsQuestion,
    onMoveIn: () -> Unit,
    onKeepHere: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = question.into ?: stringResource(R.string.merge_account_fallback)
    val held = question.fromAnotherAccount
    Column(modifier = modifier.fillMaxWidth().testTag(AccountTestTags.MERGE)) {
        Text(
            text = stringResource(if (held) R.string.merge_held_title else R.string.merge_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                if (held) R.string.merge_held_body else R.string.merge_body,
                pluralStringResource(R.plurals.account_board_count, question.boards, question.boards),
                account,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
        FilledTonalButton(
            onClick = onMoveIn,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BUTTON_HEIGHT)
                .testTag(AccountTestTags.MERGE_MOVE_IN),
        ) {
            Text(stringResource(R.string.merge_action_move_in, account), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onKeepHere,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BUTTON_HEIGHT)
                .testTag(AccountTestTags.MERGE_KEEP_HERE),
        ) {
            Text(stringResource(R.string.merge_action_keep_here), textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(if (held) R.string.merge_held_footnote else R.string.merge_footnote),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
