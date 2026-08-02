package com.artiuillab.tieryourlife.feature.tier.presentation.ui.tierlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.state.TierListsUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.viewmodel.TierListsViewModel

@Composable
fun TierListsScreen(
    onTierListClick: (Long) -> Unit,
    onToggleTheme: () -> Unit,
    viewModel: TierListsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    TierListsScreenContent(
        state = state,
        onTierListClick = onTierListClick,
        onToggleTheme = onToggleTheme,
    )
}

@Composable
internal fun TierListsScreenContent(
    state: TierListsUiState,
    onTierListClick: (Long) -> Unit,
    onToggleTheme: () -> Unit,
) {
    val lists = (state as? TierListsUiState.Success)?.lists.orEmpty()
    val rankedCount = lists.sumOf { list ->
        list.tiers.filterNot { it.isPool }.sumOf { it.items.size }
    }
    val listCountText = pluralStringResource(R.plurals.tier_lists_count, lists.size, lists.size)
    val rankingsCountText = pluralStringResource(
        R.plurals.tier_lists_rankings_count,
        rankedCount,
        rankedCount,
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                HomeTopBar(onToggleTheme = onToggleTheme)
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.tier_lists_title),
                        fontSize = 32.sp,
                        lineHeight = 40.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.tier_lists_summary,
                            listCountText,
                            rankingsCountText,
                            stringResource(R.string.tier_lists_private),
                        ),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                when (state) {
                    TierListsUiState.Loading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(Modifier.testTag("tier_lists_loading"))
                    }

                    is TierListsUiState.Error -> Text(
                        text = state.message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    is TierListsUiState.Success -> HomeContent(lists, onTierListClick)
                }
            }

            FloatingActionButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .size(56.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
            ) {
                PlusIcon(24.dp, MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun HomeTopBar(onToggleTheme: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeIconButton(stringResource(R.string.tier_lists_content_description_navigation)) { MenuIcon() }
        Spacer(Modifier.weight(1f))
        HomeIconButton(stringResource(R.string.tier_lists_content_description_search)) { SearchIcon() }
        HomeIconButton(
            stringResource(R.string.tier_lists_content_description_toggle_theme),
            onToggleTheme,
        ) { MoreIcon() }
    }
}

@Composable
private fun HomeIconButton(contentDescription: String, content: @Composable () -> Unit) {
    IconButton(
        onClick = {},
        modifier = Modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription },
    ) { content() }
}

@Composable
private fun HomeIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription },
    ) { content() }
}

@Composable
private fun HomeContent(lists: List<TierList>, onTierListClick: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tier_lists"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(lists, key = { it.id }) { list -> TierListCard(list, onTierListClick) }
        item { RecentChanges() }
    }
}

@Composable
private fun RecentChanges() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(R.string.tier_lists_recent_changes),
            modifier = Modifier.padding(start = 4.dp, top = 12.dp),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        RecentChange(
            stringResource(R.string.tier_lists_change_blade_runner),
            stringResource(R.string.tier_lists_change_sci_fi_june_12),
            true,
        )
        RecentChange(
            stringResource(R.string.tier_lists_change_first_man),
            stringResource(R.string.tier_lists_change_sci_fi_june_9),
            false,
        )
    }
}

@Composable
private fun RecentChange(title: String, subtitle: String, moved: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (moved) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (moved) UpIcon(20.dp, MaterialTheme.colorScheme.onPrimaryContainer)
            else PlusIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                title,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@TierYourLifeDevicePreviews
@Composable
private fun TierListsLightPreview() = TierYourLifeTheme(false) {
    TierListsScreenContent(TierListsUiState.Success(previewTierLists), {}, {})
}

@TierYourLifeDevicePreviews
@Composable
private fun TierListsDarkPreview() = TierYourLifeTheme(true) {
    TierListsScreenContent(TierListsUiState.Success(previewTierLists), {}, {})
}
