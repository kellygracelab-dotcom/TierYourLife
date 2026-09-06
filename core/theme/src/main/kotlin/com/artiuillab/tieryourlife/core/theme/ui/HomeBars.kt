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
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.R
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.atMost

@Composable
fun HomeTopBar(
    onSearchClick: () -> Unit,
    onSettingsClick: (() -> Unit)?,
    asPictures: Boolean = false,
    onToggleView: (() -> Unit)? = null,
) {
    // The bar spans the window; the title and the buttons it belongs with do
    // not, or the two end up a window apart.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .atMost(ContentWidth.Reading)
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_name_display),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // Null on the community half, where the feed has one shape and the
            // choice would be a control that changes nothing.
            if (onToggleView != null) {
                HomeIconButton(
                    stringResource(
                        if (asPictures) {
                            R.string.tier_lists_content_description_as_rows
                        } else {
                            R.string.tier_lists_content_description_as_pictures
                        },
                    ),
                    onToggleView,
                    HomeBarTestTags.VIEW_TOGGLE,
                ) {
                    if (asPictures) RowsIcon() else PicturesIcon()
                }
            }
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
}

@Composable
fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val closeDescription = stringResource(R.string.cd_close_search)
    val clearDescription = stringResource(R.string.cd_clear_query)
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
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
                .atMost(ContentWidth.Reading)
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
