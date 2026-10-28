package com.artiuillab.tieryourlife.feature.community.presentation.allsections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.layout.BarredScreen
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.systemBarsContentPadding
import com.artiuillab.tieryourlife.core.theme.layout.topBarHeight
import com.artiuillab.tieryourlife.core.theme.layout.topBarTitleStyle
import com.artiuillab.tieryourlife.core.theme.ui.BackIcon
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.core.theme.ui.ChevronDownIcon
import com.artiuillab.tieryourlife.core.theme.ui.PlusIcon
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.explore.Body13Style
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CommunityNotice
import com.artiuillab.tieryourlife.feature.community.presentation.explore.HomeLayout
import com.artiuillab.tieryourlife.feature.community.presentation.explore.SectionTile
import com.artiuillab.tieryourlife.feature.community.presentation.explore.WideContentMax
import com.artiuillab.tieryourlife.feature.community.presentation.explore.countText
import com.artiuillab.tieryourlife.feature.community.presentation.explore.homeLayoutFor
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.CommunityRoute
import com.artiuillab.tieryourlife.feature.tier.board.common.currentLanguageTag
import com.artiuillab.tieryourlife.feature.tier.board.common.labelWith
import com.artiuillab.tieryourlife.feature.tier.board.common.rememberSectionNames
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.text.Collator
import java.util.Locale
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

internal object AllSectionsTestTags {
    const val SCREEN = "all_sections_screen"
    const val BACK = "all_sections_back"
    const val COUNT = "all_sections_count"
    const val SORT = "all_sections_sort"
    const val FIND = "all_sections_find"
    const val NOTHING = "all_sections_nothing"
    const val PROPOSE = "all_sections_propose"

    fun sortOption(sort: SectionSort) = "all_sections_sort_${sort.name.lowercase()}"
}

/** What the screen can be asked to do; sections go by id. */
class AllSectionsCallbacks(
    val onOpenSection: (String) -> Unit = {},
    val onSort: (SectionSort) -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onFollow: (String) -> Unit = {},
    val onUnfollow: (String) -> Unit = {},
)

/**
 * [onProposeSection] is null until the proposal form exists: the button at
 * the end is drawn only where it leads somewhere.
 */
@Composable
fun AllSectionsScreen(
    onBack: () -> Unit,
    onOpenSection: (CommunityRoute.Section) -> Unit,
    onProposeSection: (() -> Unit)? = null,
    viewModel: AllSectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.open() }
    AllSectionsContent(
        state = state,
        onBack = onBack,
        calls = AllSectionsCallbacks(
            onOpenSection = { onOpenSection(CommunityRoute.Section(it)) },
            onSort = viewModel::selectSort,
            onQueryChange = viewModel::updateQuery,
            onFollow = viewModel::follow,
            onUnfollow = viewModel::unfollow,
        ),
        notices = viewModel.notices,
        onProposeSection = onProposeSection,
    )
}

/**
 * Every section, where Home has no room for them all: how many, an order to
 * choose, a search once there are more than twenty, the tiles as Home draws
 * them, and at the end the way to propose one.
 */
@Composable
fun AllSectionsContent(
    state: AllSectionsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    calls: AllSectionsCallbacks = AllSectionsCallbacks(),
    notices: Flow<CommunityNotice> = emptyFlow(),
    onProposeSection: (() -> Unit)? = null,
) {
    val snackbar = remember { SnackbarHostState() }
    AllSectionsNotices(notices, snackbar, calls.onFollow)
    Surface(modifier.fillMaxSize().testTag(AllSectionsTestTags.SCREEN), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {
            BarredScreen(bar = { TopBar(onBack) }) {
                BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    val layout = homeLayoutFor(currentWindowShape, maxHeight)
                    SectionsGrid(state, layout, calls, onProposeSection)
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Columns and tile heights as Home has them: two on a phone, three from 600, five from 840. */
private class GridShape(val columns: Int, val tileHeight: Dp, val gap: Dp, val side: Dp)

private fun gridShapeOf(layout: HomeLayout): GridShape = when (layout) {
    HomeLayout.Wide -> GridShape(columns = 5, tileHeight = 112.dp, gap = 12.dp, side = 24.dp)
    HomeLayout.Medium -> GridShape(columns = 3, tileHeight = 120.dp, gap = 12.dp, side = 16.dp)
    HomeLayout.CompactHeight -> GridShape(columns = 4, tileHeight = 88.dp, gap = 10.dp, side = 16.dp)
    HomeLayout.Cover -> GridShape(columns = 3, tileHeight = 70.dp, gap = 6.dp, side = 14.dp)
    HomeLayout.Portrait -> GridShape(columns = 2, tileHeight = 96.dp, gap = 10.dp, side = 16.dp)
}

@Composable
private fun SectionsGrid(state: AllSectionsUiState, layout: HomeLayout, calls: AllSectionsCallbacks, onProposeSection: (() -> Unit)?) {
    val shape = gridShapeOf(layout)
    val resources = LocalResources.current
    val tag = currentLanguageTag()
    val shown = remember(state, resources, tag) {
        val collator = Collator.getInstance(Locale.forLanguageTag(tag))
        allSectionsInOrder(state.sections, state.sort, state.query, { it.labelWith(resources, tag) }, collator)
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(shape.columns),
        modifier = Modifier
            .then(if (layout == HomeLayout.Wide) Modifier.widthIn(max = WideContentMax) else Modifier)
            .fillMaxSize(),
        contentPadding = systemBarsContentPadding(start = shape.side, end = shape.side, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(shape.gap),
        verticalArrangement = Arrangement.spacedBy(shape.gap),
    ) {
        item(key = "head", span = { GridItemSpan(maxLineSpan) }) {
            CountAndSort(state.sections.size, state.sort, calls.onSort)
        }
        if (state.searchable) {
            item(key = "find", span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = calls.onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AllSectionsTestTags.FIND),
                    placeholder = { Text(stringResource(R.string.all_sections_find)) },
                    singleLine = true,
                )
            }
        }
        if (shown.isEmpty()) {
            item(key = "nothing", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.explore_search_nothing),
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .testTag(AllSectionsTestTags.NOTHING),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(shown, key = { it.id }) { section ->
            SectionTile(
                section = section,
                newThisWeek = section.newThisWeek.takeIf { state.countsKnown && !section.isOther },
                following = section.following,
                onOpen = { calls.onOpenSection(section.id) },
                onFollow = { calls.onFollow(section.id) },
                onUnfollow = { calls.onUnfollow(section.id) },
                modifier = Modifier.height(shape.tileHeight),
            )
        }
        if (onProposeSection != null) {
            item(key = "propose", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                    OutlinedButton(onClick = onProposeSection, modifier = Modifier.height(40.dp).testTag(AllSectionsTestTags.PROPOSE)) {
                        PlusIcon(18.dp, MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.all_sections_propose))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val backDescription = stringResource(ThemeR.string.tier_detail_content_description_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(topBarHeight())
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = backDescription }
                .testTag(AllSectionsTestTags.BACK),
        ) { BackIcon() }
        Text(
            text = stringResource(R.string.all_sections_title),
            modifier = Modifier.padding(horizontal = 4.dp),
            style = topBarTitleStyle(),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** "14 sections", and Most lists or A–Z from a menu. */
@Composable
private fun CountAndSort(count: Int, sort: SectionSort, onSort: (SectionSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            countText(R.plurals.all_sections_count, count),
            modifier = Modifier
                .weight(1f)
                .testTag(AllSectionsTestTags.COUNT),
            style = Body13Style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            TextButton(onClick = { open = true }, modifier = Modifier.testTag(AllSectionsTestTags.SORT)) {
                Text(stringResource(sort.labelRes), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                ChevronDownIcon(22.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                SectionSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes)) },
                        trailingIcon = { if (option == sort) CheckIcon(18.dp, MaterialTheme.colorScheme.primary) },
                        onClick = {
                            open = false
                            onSort(option)
                        },
                        modifier = Modifier.testTag(AllSectionsTestTags.sortOption(option)),
                    )
                }
            }
        }
    }
}

private val SectionSort.labelRes: Int
    get() = if (this == SectionSort.MostLists) R.string.all_sections_sort_most_lists else R.string.all_sections_sort_name

@Composable
private fun AllSectionsNotices(notices: Flow<CommunityNotice>, snackbar: SnackbarHostState, onFollowAgain: (String) -> Unit) {
    val resources = LocalResources.current
    val nameOf = rememberSectionNames()
    LaunchedEffect(notices) {
        notices.collect { notice ->
            when (notice) {
                is CommunityNotice.NowFollowing -> snackbar.showSnackbar(resources.getString(R.string.explore_now_following, nameOf(notice.sectionId)))
                is CommunityNotice.Unfollowed -> {
                    val result = snackbar.showSnackbar(
                        message = resources.getString(R.string.explore_unfollowed, nameOf(notice.sectionId)),
                        actionLabel = resources.getString(R.string.explore_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) onFollowAgain(notice.sectionId)
                }

                CommunityNotice.FollowFailed -> snackbar.showSnackbar(resources.getString(R.string.explore_follow_failed))
                CommunityNotice.ActionFailed -> snackbar.showSnackbar(resources.getString(ThemeR.string.snack_action_failed))
                is CommunityNotice.RefreshFailed -> Unit
            }
        }
    }
}
