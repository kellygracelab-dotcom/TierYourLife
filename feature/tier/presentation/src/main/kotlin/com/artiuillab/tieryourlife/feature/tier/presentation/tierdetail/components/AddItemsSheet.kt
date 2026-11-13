package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.layout.EndSideSheet
import com.artiuillab.tieryourlife.core.theme.layout.SheetWidth
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchViewModel
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.ScopeChoice
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components.LinkChoice
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components.attributionWith

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddItemsSheet(
    listTitle: String,
    /** The board's section: its own catalogues are searched first. */
    category: String?,
    onDismiss: () -> Unit,
    onItemsConfirmed: (List<PoolItemDraft>) -> Unit,
    /** From the end edge, the board left in view: for a board laid out in columns. */
    side: Boolean = false,
    /** A picture from the device instead, when pictures from links are used up for the day. */
    onUploadInstead: () -> Unit = {},
    onSignIn: () -> Unit = {},
    searchViewModel: CatalogueSearchViewModel = hiltViewModel(),
) {
    val searchState by searchViewModel.state.collectAsStateWithLifecycle()
    val scope by searchViewModel.scope.collectAsStateWithLifecycle()
    LaunchedEffect(category) { searchViewModel.start(category) }
    var query by rememberSaveable { mutableStateOf("") }
    val link by searchViewModel.link.collectAsStateWithLifecycle()
    val clipboard = rememberClipboardLinks()
    LifecycleResumeEffect(Unit) {
        clipboard.look()
        searchViewModel.retryLink()
        onPauseOrDispose {}
    }

    var selectedItems by remember { mutableStateOf<Map<String, CatalogueItem>>(emptyMap()) }

    val content: @Composable ColumnScope.() -> Unit = {
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
            onConfirmSelection = { onItemsConfirmed(selectedItems.values.map { it.draft() }) },
            onLoadMore = searchViewModel::loadMore,
            scopes = ScopeChoice(first = PictureScope.forCategory(category), selected = scope, onSelect = searchViewModel::setScope),
            onSearchEverywhere = searchViewModel::searchEverywhere,
            links = LinkChoice(
                state = link,
                pasteOffered = clipboard.offered,
                onPaste = {
                    clipboard.paste()?.let {
                        query = it
                        searchViewModel.search(it)
                    }
                },
                onAdd = { title, found, page ->
                    onItemsConfirmed(
                        selectedItems.values.map { it.draft() } +
                            PoolItemDraft(title = title, imageUrl = found.imageUrl, credit = found.credit, attribution = found.attributionWith(page)),
                    )
                },
                onAddWithoutPicture = { title ->
                    onItemsConfirmed(selectedItems.values.map { it.draft() } + PoolItemDraft(title = title, imageUrl = null))
                },
                onUpload = onUploadInstead,
                onSignIn = onSignIn,
                onRetry = searchViewModel::retryLink,
            ),
        )
    }
    if (side) {
        EndSideSheet(onDismiss = onDismiss, content = content)
    } else {
        AdaptiveSheet(
            onDismiss = onDismiss,
            width = SheetWidth.Working,
            maxHeight = 640.dp,
            content = content,
        )
    }
}

private fun CatalogueItem.draft() = PoolItemDraft(title = title, imageUrl = imageUrl, keepCopy = fromWeb, credit = credit, attribution = attribution)
