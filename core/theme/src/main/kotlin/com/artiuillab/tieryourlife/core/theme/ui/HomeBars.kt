package com.artiuillab.tieryourlife.core.theme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.R
import com.artiuillab.tieryourlife.core.theme.layout.ColumnAlignedTopBar
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.TopBarDefaults
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.core.theme.layout.isShortWindow
import com.artiuillab.tieryourlife.core.theme.layout.overhang

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

/**
 * The bar while searching: back, then the field, on the same column as the
 * results (§2.3). Slim in a short window like every other bar there; the
 * field itself is the same either way.
 */
@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    hint: String? = null,
    contentMax: Dp = ContentWidth.Reading,
    statusBarPadding: Boolean = true,
    compact: Boolean = isShortWindow,
) {
    val closeDescription = stringResource(R.string.cd_close_search)
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .let { if (statusBarPadding) it.statusBarsPadding() else it },
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .atMost(contentMax)
                .padding(horizontal = TopBarDefaults.ContentPadding)
                .height(if (compact) TopBarDefaults.CompactHeight else TopBarDefaults.Height),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.overhang(start = true)) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = closeDescription }
                        .testTag(HomeBarTestTags.SEARCH_CLOSE),
                ) { BackIcon() }
            }
            Spacer(Modifier.width(4.dp))
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                hint = hint ?: stringResource(R.string.home_search_hint),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag(HomeBarTestTags.SEARCH_FIELD),
            )
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/** 40 high and fully rounded, a shade off the bar so it reads as a field without a border. */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, hint: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val clearDescription = stringResource(R.string.cd_clear_query)
    val keyboard = LocalSoftwareKeyboardController.current
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        textStyle = SearchQueryStyle.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        // Done typing is not done searching: the results stay, the keyboard goes.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        decorationBox = { field ->
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceContainerHighest)
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = hint,
                            style = SearchQueryStyle,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    field()
                }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { onQueryChange("") }
                            .semantics { contentDescription = clearDescription }
                            .testTag(HomeBarTestTags.SEARCH_CLEAR),
                        contentAlignment = Alignment.Center,
                    ) { ClearIcon(20.dp, colors.onSurfaceVariant) }
                } else {
                    Spacer(Modifier.width(16.dp))
                }
            }
        },
    )
}

private val SearchQueryStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)

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
