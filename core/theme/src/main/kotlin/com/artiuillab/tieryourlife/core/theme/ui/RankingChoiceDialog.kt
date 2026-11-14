package com.artiuillab.tieryourlife.core.theme.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.artiuillab.tieryourlife.core.theme.R

/** Asked once, after a game or with every card placed; nothing goes public without Show in Rankings. */
@Composable
fun RankingChoiceDialog(
    signedIn: Boolean,
    busy: Boolean,
    onShow: () -> Unit,
    onNotNow: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag("ranking_choice_dialog"),
        onDismissRequest = { if (!busy) onNotNow() },
        title = { Text(stringResource(R.string.ranking_offer_title)) },
        confirmButton = {
            TextButton(onClick = onShow, enabled = !busy, modifier = Modifier.testTag("ranking_choice_public")) {
                Text(stringResource(if (signedIn) R.string.ranking_offer_show else R.string.ranking_offer_sign_in))
            }
        },
        dismissButton = {
            TextButton(onClick = onNotNow, enabled = !busy, modifier = Modifier.testTag("ranking_choice_not_now")) {
                Text(stringResource(R.string.ranking_offer_not_now))
            }
        },
    )
}
