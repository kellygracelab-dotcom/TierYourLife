package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components.CaptionLine
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components.CenteredMessage
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components.ResultsList
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components.SearchField
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components.SelectionBar

private val FieldFillLight = Color(0xFFEFEDF4)
private val FieldFillDark = Color(0xFF2A2A31)
private val SelectedRowTintLight = Color(0xFFEDEBFA)
private val SelectedRowTintDark = Color(0xFF2E2F45)

@Composable
fun CatalogueSearchScreenContent(
    state: CatalogueSearchUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onClose: () -> Unit = {},
    listTitle: String = "",
    selectedIds: Set<String> = emptySet(),
    onToggleSelection: (CatalogueItem) -> Unit = {},
    onConfirmSelection: () -> Unit = {},
) {
    val isDark = TierYourLifeMedia.current.isDark
    val fieldFill = if (isDark) FieldFillDark else FieldFillLight

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .imePadding(),
    ) {
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            onSearchClick = onSearchClick,
            onClose = onClose,
            fill = fieldFill,
        )

        if (state is CatalogueSearchUiState.Loading) {
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            )
        }

        CaptionLine(listTitle = listTitle)

        Box(modifier = Modifier.weight(1f)) {
            when (state) {
                is CatalogueSearchUiState.Initial -> {
                    CenteredMessage(text = stringResource(R.string.item_search_min_query))
                }

                is CatalogueSearchUiState.Loading -> {
                }

                is CatalogueSearchUiState.Empty -> {
                    CenteredMessage(
                        text = stringResource(R.string.item_search_empty_title, state.query),
                    )
                }

                is CatalogueSearchUiState.Success -> {
                    ResultsList(
                        items = state.items,
                        selectedIds = selectedIds,
                        selectedTint = if (isDark) SelectedRowTintDark else SelectedRowTintLight,
                        onToggle = onToggleSelection,
                    )
                }

                CatalogueSearchUiState.Error -> {
                    CenteredMessage(
                        text = stringResource(R.string.item_search_error_title),
                        body = stringResource(R.string.item_search_error_body),
                        actionLabel = stringResource(R.string.item_search_try_again),
                        onAction = onSearchClick,
                    )
                }
            }
        }

        SelectionBar(
            selectedCount = selectedIds.size,
            fill = fieldFill,
            onConfirm = onConfirmSelection,
        )
    }
}

private val previewItems = listOf(
    CatalogueItem(id = "tmdb:1", title = "The Godfather", subtitle = "1972", imageUrl = null),
    CatalogueItem(id = "tmdb:2", title = "Inception", subtitle = "2010", imageUrl = null),
)

@Preview(name = "Initial", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun CatalogueSearchScreenInitialPreview() {
    TierYourLifeTheme {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Initial,
            query = "",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Sci-fi films",
        )
    }
}

@Preview(name = "Loading", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun CatalogueSearchScreenLoadingPreview() {
    TierYourLifeTheme {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Loading,
            query = "Interstellar",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Sci-fi films",
        )
    }
}

@Preview(name = "Success", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun CatalogueSearchScreenSuccessPreview() {
    TierYourLifeTheme {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Success(items = previewItems),
            query = "Godfather",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Sci-fi films",
            selectedIds = setOf("tmdb:1"),
        )
    }
}

@Preview(name = "Empty", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun CatalogueSearchScreenEmptyPreview() {
    TierYourLifeTheme {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Empty(query = "zzxxccvv"),
            query = "zzxxccvv",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Sci-fi films",
        )
    }
}

@Preview(name = "Error", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun CatalogueSearchScreenErrorPreview() {
    TierYourLifeTheme {
        CatalogueSearchScreenContent(
            state = CatalogueSearchUiState.Error,
            query = "Interstellar",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Sci-fi films",
        )
    }
}
