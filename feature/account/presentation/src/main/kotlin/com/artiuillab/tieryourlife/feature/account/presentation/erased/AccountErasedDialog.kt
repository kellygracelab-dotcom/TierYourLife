package com.artiuillab.tieryourlife.feature.account.presentation.erased

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.settings.AccountErasedNotice
import com.artiuillab.tieryourlife.feature.account.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

const val ACCOUNT_ERASED_DIALOG = "account_erased_dialog"

@HiltViewModel
class AccountErasedViewModel @Inject constructor(private val notice: AccountErasedNotice) : ViewModel() {

    val pending: StateFlow<AccountErasedNotice.Where?> = notice.pending

    fun dismiss() = notice.dismiss()
}

/** Over whatever screen is open: the account can go from under any of them. */
@Composable
fun AccountErasedNoticeHost() {
    val model: AccountErasedViewModel = hiltViewModel()
    val pending by model.pending.collectAsStateWithLifecycle()
    pending?.let { where -> AccountErasedDialog(where, onDismiss = model::dismiss) }
}

@Composable
internal fun AccountErasedDialog(where: AccountErasedNotice.Where, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(ACCOUNT_ERASED_DIALOG),
        title = { Text(stringResource(R.string.account_erased_title)) },
        text = {
            Text(
                text = stringResource(
                    when (where) {
                        AccountErasedNotice.Where.ThisDevice -> R.string.account_erased_here_body
                        AccountErasedNotice.Where.AnotherDevice -> R.string.account_erased_body
                    },
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.account_erased_ok)) }
        },
    )
}
