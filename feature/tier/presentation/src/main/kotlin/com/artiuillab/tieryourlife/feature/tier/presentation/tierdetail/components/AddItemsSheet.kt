package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddItemsSheet(
    listTitle: String,
    onDismiss: () -> Unit,
    onItemsConfirmed: (List<PoolItemDraft>) -> Unit,
    searchViewModel: CatalogueSearchViewModel = hiltViewModel(),
) {
    val searchState by searchViewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    var selectedItems by remember { mutableStateOf<Map<String, CatalogueItem>>(emptyMap()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        CatalogueSearchScreenContent(
            state = searchState,
            query = query,
            onQueryChange = {
                query = it
                searchViewModel.onQueryChange(it)
            },
            onSearchClick = { searchViewModel.search(query) },
            onClose = onDismiss,
            listTitle = listTitle,
            selectedIds = selectedItems.keys,
            onToggleSelection = { item ->
                selectedItems = if (selectedItems.containsKey(item.id)) {
                    selectedItems - item.id
                } else {
                    selectedItems + (item.id to item)
                }
            },
            onConfirmSelection = {
                onItemsConfirmed(selectedItems.values.map { PoolItemDraft(title = it.title, imageUrl = it.imageUrl) })
            },
        )
    }
}
