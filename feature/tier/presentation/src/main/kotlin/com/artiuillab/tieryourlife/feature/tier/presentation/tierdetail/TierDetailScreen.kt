package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.MoreIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.AddMovieSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.FloatingDragTile
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.NoteAddIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.PoolPanel
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.TierDragController
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.TierRow
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.previewTierList

// testTag constants shared between production UI and instrumentation tests, so
// the two never drift out of sync.
internal object TierDetailTestTags {
    const val LOADING = "tier_detail_loading"
    const val ADD_CHIP = "tier_detail_add_chip"
    const val POOL_ITEMS = "tier_detail_pool_items"
    const val POOL_PANEL = "tier_detail_pool_panel"
    fun tierItems(tierId: Long): String = "tier_detail_items_$tierId"
    fun tile(itemId: Long): String = "tier_detail_tile_$itemId"
}

@Composable
fun TierDetailScreen(
    onBack: () -> Unit,
    viewModel: TierDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var addSheetVisible by rememberSaveable { mutableStateOf(false) }

    TierDetailScreenContent(
        state = state,
        onBack = onBack,
        onAddClick = { addSheetVisible = true },
        onMoveItem = viewModel::moveItem,
    )

    if (addSheetVisible) {
        AddMovieSheet(
            onDismiss = { addSheetVisible = false },
            onMovieSelected = { title, imageUrl ->
                viewModel.addMovieToPool(title, imageUrl)
                addSheetVisible = false
            },
        )
    }
}

@Composable
fun TierDetailScreenContent(
    state: TierDetailUiState,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit = { _, _, _ -> },
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        when (state) {
            is TierDetailUiState.Loading -> {
                Column(Modifier.fillMaxSize()) {
                    TierScreenTopBar(title = "", onBack = onBack, onManualAdd = onAddClick)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(TierDetailTestTags.LOADING),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is TierDetailUiState.Success -> {
                TierScreenBody(list = state.list, onBack = onBack, onAddClick = onAddClick, onMoveItem = onMoveItem)
            }

            is TierDetailUiState.Error -> {
                Column {
                    TierScreenTopBar(title = "", onBack = onBack, onManualAdd = onAddClick)
                    Text(
                        text = state.message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun TierScreenBody(
    list: TierList,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
) {
    val rankedTiers = list.tiers.filterNot { it.isPool }
    val pool = list.tiers.firstOrNull { it.isPool }
    val dragController = remember { TierDragController() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TierScreenTopBar(title = list.title, onBack = onBack, onManualAdd = onAddClick)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rankedTiers, key = { it.id }) { tier ->
                    TierRow(tier = tier, dragController = dragController, onMoveItem = onMoveItem)
                }
            }

            if (pool != null) {
                PoolPanel(pool = pool, onAddClick = onAddClick, dragController = dragController, onMoveItem = onMoveItem)
            }
        }

        if (dragController.isDragging) {
            FloatingDragTile(dragController)
        }
    }
}

@Composable
private fun TierScreenTopBar(title: String, onBack: () -> Unit, onManualAdd: () -> Unit) {
    val backDescription = stringResource(R.string.tier_detail_content_description_back)
    val manualAddDescription = stringResource(R.string.tier_detail_content_description_manual_add)
    val moreDescription = stringResource(R.string.tier_detail_content_description_more)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = backDescription },
        ) { BackIcon() }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            fontSize = 20.sp,
            lineHeight = 28.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(
            onClick = onManualAdd,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = manualAddDescription },
        ) { NoteAddIcon() }
        IconButton(
            onClick = {},
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = moreDescription },
        ) { MoreIcon() }
    }
}

@TierYourLifeDevicePreviews
@Composable
private fun TierScreenLightPreview() = TierYourLifeTheme(false) {
    TierDetailScreenContent(state = TierDetailUiState.Success(previewTierList), onBack = {}, onAddClick = {})
}

@TierYourLifeDevicePreviews
@Composable
private fun TierScreenDarkPreview() = TierYourLifeTheme(true) {
    TierDetailScreenContent(state = TierDetailUiState.Success(previewTierList), onBack = {}, onAddClick = {})
}

@Preview(name = "Loading", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun TierScreenLoadingPreview() {
    TierYourLifeTheme {
        TierDetailScreenContent(state = TierDetailUiState.Loading, onBack = {}, onAddClick = {})
    }
}

@Preview(name = "Error", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun TierScreenErrorPreview() {
    TierYourLifeTheme {
        TierDetailScreenContent(
            state = TierDetailUiState.Error(message = "No connection to server"),
            onBack = {},
            onAddClick = {},
        )
    }
}
