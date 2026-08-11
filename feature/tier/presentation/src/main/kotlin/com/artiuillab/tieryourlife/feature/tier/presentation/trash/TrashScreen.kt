package com.artiuillab.tieryourlife.feature.tier.presentation.trash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.components.EmptyTrashDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.components.RemoveConfirmDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.components.TrashEmptyState
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.components.TrashList
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.components.TrashTopBar

@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TrashScreenContent(
        state = state,
        onBack = onBack,
        onRestoreList = viewModel::restoreList,
        onRestoreItem = viewModel::restoreItem,
        onRemoveListPermanently = viewModel::removeListPermanently,
        onRemoveItemPermanently = viewModel::removeItemPermanently,
        onEmptyTrash = viewModel::emptyTrash,
    )
}

@Composable
internal fun TrashScreenContent(
    state: TrashUiState,
    onBack: () -> Unit,
    onRestoreList: (Long) -> Unit = {},
    onRestoreItem: (Long) -> Unit = {},
    onRemoveListPermanently: (Long) -> Unit = {},
    onRemoveItemPermanently: (Long) -> Unit = {},
    onEmptyTrash: () -> Unit = {},
) {
    val entries = (state as? TrashUiState.Success)?.entries.orEmpty()
    var menuExpanded by remember { mutableStateOf(false) }
    var emptyDialogVisible by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<TrashEntry?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize()) {
            TrashTopBar(
                isEmpty = entries.isEmpty(),
                menuExpanded = menuExpanded,
                onBack = onBack,
                onMoreClick = { menuExpanded = true },
                onDismissMenu = { menuExpanded = false },
                onEmptyTrashClick = {
                    menuExpanded = false
                    emptyDialogVisible = true
                },
            )

            when (state) {
                TrashUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                TrashUiState.Error -> Text(
                    text = stringResource(R.string.trash_load_error),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                is TrashUiState.Success -> if (entries.isEmpty()) {
                    TrashEmptyState()
                } else {
                    TrashList(
                        entries = entries,
                        onRestoreList = onRestoreList,
                        onRestoreItem = onRestoreItem,
                        onRemoveRequested = { removeTarget = it },
                    )
                }
            }
        }
    }

    val target = removeTarget
    if (target != null) {
        RemoveConfirmDialog(
            entry = target,
            onDismiss = { removeTarget = null },
            onConfirm = {
                when (target) {
                    is TrashEntry.DeletedList -> onRemoveListPermanently(target.id)
                    is TrashEntry.DeletedItem -> onRemoveItemPermanently(target.id)
                }
                removeTarget = null
            },
        )
    }

    if (emptyDialogVisible) {
        EmptyTrashDialog(
            entryCount = entries.size,
            onDismiss = { emptyDialogVisible = false },
            onConfirm = {
                onEmptyTrash()
                emptyDialogVisible = false
            },
        )
    }
}

@TierYourLifeDevicePreviews
@Composable
private fun TrashScreenLightPreview() = TierYourLifeTheme(false) {
    TrashScreenContent(
        state = TrashUiState.Success(previewTrashEntries),
        onBack = {},
    )
}

@TierYourLifeDevicePreviews
@Composable
private fun TrashScreenDarkPreview() = TierYourLifeTheme(true) {
    TrashScreenContent(
        state = TrashUiState.Success(previewTrashEntries),
        onBack = {},
    )
}
