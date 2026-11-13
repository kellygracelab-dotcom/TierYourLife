package com.artiuillab.tieryourlife.feature.tier.presentation.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkPicture
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components.LinkStatus
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.components.relativeTimeText

object AddToListTestTags {
    const val SHEET = "add_to_list_sheet"
    const val NAME = "add_to_list_name"
    const val NEW_LIST = "add_to_list_new"
    const val NEW_LIST_NAME = "add_to_list_new_name"
    const val TO_POOL = "add_to_list_to_pool"
    fun list(id: Long) = "add_to_list_list_$id"
}

/** "Add to list", what a browser's Share opens: the picture, its name, and which list's pool it goes to. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToListSheet(
    shared: Shared,
    onClose: () -> Unit,
    onAdded: (listTitle: String) -> Unit,
    viewModel: AddToListViewModel = hiltViewModel(),
) {
    LaunchedEffect(shared) { viewModel.start(shared) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val newListTitle = stringResource(R.string.default_tier_list_title)
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        AddToListContent(
            state = state,
            shared = shared,
            defaultNewListTitle = newListTitle,
            onChoose = viewModel::choose,
            onCancel = onClose,
            onAdd = { title, listTitle -> viewModel.add(title, listTitle, onAdded) },
        )
    }
}

@Composable
fun AddToListContent(
    state: AddToListUiState,
    shared: Shared,
    defaultNewListTitle: String,
    onChoose: (Long?) -> Unit,
    onCancel: () -> Unit,
    onAdd: (title: String, newListTitle: String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(shared.subject?.trim()?.take(MAX_TITLE).orEmpty()) }
    var newListTitle by rememberSaveable { mutableStateOf(defaultNewListTitle) }
    val found = state.picture is LinkPicture.Found
    val canAdd = !state.saving && state.canAdd(title) && (state.chosen != null || newListTitle.isNotBlank())

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .testTag(AddToListTestTags.SHEET),
    ) {
        Text(
            stringResource(R.string.add_to_list_title), style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 14.dp),
        )
        PictureAndName(state, shared.host, title) { title = it }
        Text(
            stringResource(R.string.add_to_list_where),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 18.dp),
        )
        ListChooser(state, onChoose)
        if (state.chosen == null) {
            OutlinedTextField(
                value = newListTitle,
                onValueChange = { newListTitle = it },
                label = { Text(stringResource(R.string.add_to_list_new_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(AddToListTestTags.NEW_LIST_NAME),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp),
        ) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.add_to_list_cancel)) }
            Button(
                onClick = { onAdd(title.trim(), newListTitle.trim()) },
                enabled = canAdd,
                modifier = Modifier.testTag(AddToListTestTags.TO_POOL),
            ) {
                Text(stringResource(if (found || !state.hasPicture) R.string.add_to_list_to_pool else R.string.item_search_link_add_without_picture))
            }
        }
    }
}

/** A picture goes in as it is; when none came, the card still can, with its name. */
private fun AddToListUiState.canAdd(title: String): Boolean =
    picture is LinkPicture.Found || ((picture != null || !hasPicture) && title.isNotBlank())

@Composable
private fun PictureAndName(state: AddToListUiState, host: String?, title: String, onTitle: (String) -> Unit) {
    val picture = state.picture
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.hasPicture) Preview(picture)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitle,
                label = { Text(stringResource(R.string.item_search_link_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(AddToListTestTags.NAME),
            )
            host?.let {
                Text(stringResource(R.string.add_to_list_from, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                !state.hasPicture || picture is LinkPicture.Found -> Unit
                // Outside the app there is no sign-in to offer: it says why, and the card can still go without its picture.
                picture == LinkPicture.SignInNeeded -> Text(
                    stringResource(R.string.add_to_list_sign_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                else -> LinkStatus(picture)
            }
        }
    }
}

@Composable
private fun ListChooser(state: AddToListUiState, onChoose: (Long?) -> Unit) {
    Column(Modifier.selectableGroup()) {
        state.lists.forEach { list ->
            ListRow(
                title = list.title,
                detail = list.editedAt?.let { relativeTimeText(it) },
                selected = state.chosen == list.id,
                onClick = { onChoose(list.id) },
                tag = AddToListTestTags.list(list.id),
            )
        }
        ListRow(
            title = stringResource(R.string.add_to_list_new),
            detail = null,
            selected = state.chosen == null,
            onClick = { onChoose(null) },
            tag = AddToListTestTags.NEW_LIST,
        )
    }
}

@Composable
private fun Preview(picture: LinkPicture?) {
    Box(
        modifier = Modifier
            .width(110.dp)
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        when (picture) {
            null -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            is LinkPicture.Found -> AsyncImage(model = picture.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth())
            else -> Unit
        }
    }
}

@Composable
private fun ListRow(title: String, detail: String?, selected: Boolean, onClick: () -> Unit, tag: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp)
            .testTag(tag),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private const val MAX_TITLE = 120
