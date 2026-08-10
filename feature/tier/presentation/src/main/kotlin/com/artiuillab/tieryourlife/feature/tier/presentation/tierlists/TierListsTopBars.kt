package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.ClearIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeleteOutlineIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.SearchIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.SettingsIcon

@Composable
internal fun HomeHeader(totalListCount: Int, rankedCount: Int) {
    Column(
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.tier_lists_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (totalListCount > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.tier_lists_summary,
                    pluralStringResource(R.plurals.tier_lists_count, totalListCount, totalListCount),
                    pluralStringResource(R.plurals.tier_lists_rankings_count, rankedCount, rankedCount),
                    stringResource(R.string.tier_lists_private),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun HomeTopBar(onSearchClick: () -> Unit, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        HomeIconButton(stringResource(R.string.tier_lists_content_description_search), onSearchClick) {
            SearchIcon()
        }
        HomeIconButton(stringResource(R.string.tier_lists_content_description_settings), onSettingsClick) {
            SettingsIcon()
        }
    }
}

@Composable
internal fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val closeDescription = stringResource(R.string.cd_close_search)
    val clearDescription = stringResource(R.string.cd_clear_query)
    val focusRequester = remember { FocusRequester() }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.home_search_hint)) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        leadingIcon = {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .semantics { contentDescription = closeDescription }
                    .testTag(TierListsTestTags.SEARCH_CLOSE),
            ) { BackIcon() }
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier
                        .semantics { contentDescription = clearDescription }
                        .testTag(TierListsTestTags.SEARCH_CLEAR),
                ) { ClearIcon(20.dp, onSurfaceVariant) }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
            .height(56.dp)
            .focusRequester(focusRequester)
            .testTag(TierListsTestTags.SEARCH_FIELD),
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
internal fun SelectionTopBar(count: Int, onClose: () -> Unit, onDelete: () -> Unit) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp)
                .testTag(TierListsTestTags.SELECTION_BAR),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeIconButton(
                stringResource(R.string.cd_close_selection),
                onClose,
                TierListsTestTags.SELECTION_CLOSE,
            ) { ClearIcon(24.dp, onSurfaceVariant) }
            Text(
                text = pluralStringResource(R.plurals.selection_count, count, count),
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val deleteDescription = stringResource(
                R.string.cd_delete_selected,
                pluralStringResource(R.plurals.tier_lists_count, count, count),
            )
            HomeIconButton(deleteDescription, onDelete, TierListsTestTags.SELECTION_DELETE) {
                DeleteOutlineIcon(24.dp, onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HomeIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String? = null,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription }
            .let { if (testTag != null) it.testTag(testTag) else it },
    ) { content() }
}
