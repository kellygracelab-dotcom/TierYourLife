package com.artiuillab.tieryourlife.core.theme.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.R
import com.artiuillab.tieryourlife.core.theme.layout.ColumnAlignedTopBar
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.atMost

/**
 * The bar over a home half. It stands on the column of what is under it
 * ([contentMax]), so the title and its buttons stay together on a wide window.
 * [statusBarPadding] is false where the screen around it keeps the status bar
 * clear itself, as a [com.artiuillab.tieryourlife.core.theme.layout.BarredScreen] does.
 */
@Composable
fun HomeTopBar(
    onSearchClick: () -> Unit,
    onSettingsClick: (() -> Unit)?,
    title: String = stringResource(R.string.app_name_display),
    count: String? = null,
    contentMax: Dp = ContentWidth.Reading,
    statusBarPadding: Boolean = true,
) {
    ColumnAlignedTopBar(
        title = title,
        modifier = if (statusBarPadding) Modifier.statusBarsPadding() else Modifier,
        contentMax = contentMax,
        count = count,
        titleTestTag = HomeBarTestTags.TITLE,
    ) {
        HomeIconButton(stringResource(R.string.tier_lists_content_description_search), onSearchClick) {
            SearchIcon()
        }
        // Null once the rail is carrying it. Two ways to the same screen,
        // one of them left over from a narrower window, is how a tablet
        // layout starts looking unconsidered.
        if (onSettingsClick != null) {
            HomeIconButton(
                stringResource(R.string.tier_lists_content_description_settings),
                onSettingsClick,
            ) {
                SettingsIcon()
            }
        }
    }
}

@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    contentMax: Dp = ContentWidth.Reading,
    statusBarPadding: Boolean = true,
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val closeDescription = stringResource(R.string.cd_close_search)
    val clearDescription = stringResource(R.string.cd_clear_query)
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (statusBarPadding) it.statusBarsPadding() else it },
        contentAlignment = Alignment.TopCenter,
    ) {
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
                        .testTag(HomeBarTestTags.SEARCH_CLOSE),
                ) { BackIcon() }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier
                            .semantics { contentDescription = clearDescription }
                            .testTag(HomeBarTestTags.SEARCH_CLEAR),
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
                .atMost(contentMax)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
                .height(56.dp)
                .focusRequester(focusRequester)
                .testTag(HomeBarTestTags.SEARCH_FIELD),
        )
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
fun HomeIconButton(
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
