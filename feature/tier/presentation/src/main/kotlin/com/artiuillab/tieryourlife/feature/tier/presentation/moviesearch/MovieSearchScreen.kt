package com.artiuillab.tieryourlife.feature.tier.presentation.moviesearch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.ClearIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CheckIcon

// Mock-literal colours with no single matching M3 role (see docs/design-spec-turns-8-9.md
// §10.2/§10.6/§10.7) — the field/bar fill's dark half matches surfaceContainerHigh but its
// light half matches surfaceContainer, and the selected-row tint matches neither; both are
// hardcoded the same way MoveItemSheet's current-tier tint already is.
private val FieldFillLight = Color(0xFFEFEDF4)
private val FieldFillDark = Color(0xFF2A2A31)
private val SelectedRowTintLight = Color(0xFFEDEBFA)
private val SelectedRowTintDark = Color(0xFF2E2F45)

// The selection map lives one level up, in whatever hosts this composable (AddMovieSheet in
// production) — see that file's comment. This composable is deliberately stateless about it.
@Composable
fun MovieSearchScreenContent(
    state: MovieSearchUiState,
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

        if (state is MovieSearchUiState.Loading) {
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
                is MovieSearchUiState.Initial -> {
                    CenteredMessage(text = stringResource(R.string.movie_search_min_query))
                }

                is MovieSearchUiState.Loading -> {
                    // No spinner and no skeleton rows here — the 4dp bar above the caption
                    // line is the only loading indicator; a centred one here would flash the
                    // list empty on every keystroke's debounced request.
                }

                is MovieSearchUiState.Empty -> {
                    CenteredMessage(
                        text = stringResource(R.string.movie_search_empty_title, state.query),
                    )
                }

                is MovieSearchUiState.Success -> {
                    MovieResultsList(
                        items = state.items,
                        selectedIds = selectedIds,
                        selectedTint = if (isDark) SelectedRowTintDark else SelectedRowTintLight,
                        onToggle = onToggleSelection,
                    )
                }

                is MovieSearchUiState.Error -> {
                    CenteredMessage(
                        text = stringResource(R.string.movie_search_error_title),
                        body = stringResource(R.string.movie_search_error_body),
                        actionLabel = stringResource(R.string.movie_search_try_again),
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

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onClose: () -> Unit,
    fill: Color,
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val closeDescription = stringResource(R.string.movie_search_close_content_description)
    val clearDescription = stringResource(R.string.movie_search_clear_content_description)

    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.movie_search_field_label)) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        leadingIcon = {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .semantics { contentDescription = closeDescription }
                    .testTag(TierDetailTestTags.MOVIE_SEARCH_CLOSE),
            ) { BackIcon() }
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier
                        .semantics { contentDescription = clearDescription }
                        .testTag(TierDetailTestTags.MOVIE_SEARCH_CLEAR),
                ) { ClearIcon(20.dp, onSurfaceVariant) }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = fill,
            unfocusedContainerColor = fill,
            disabledContainerColor = fill,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchClick() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
            .height(56.dp)
            .testTag(TierDetailTestTags.MOVIE_SEARCH_FIELD),
    )
}

@Composable
private fun CaptionLine(listTitle: String) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val fullText = stringResource(R.string.movie_search_source_caption, listTitle)
    val listNameStart = fullText.indexOf(listTitle).takeIf { listTitle.isNotEmpty() && it >= 0 }
    val annotated = buildAnnotatedString {
        append(fullText)
        listNameStart?.let { start ->
            addStyle(SpanStyle(color = primary), start, start + listTitle.length)
        }
    }
    Text(
        text = annotated,
        color = onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp).padding(bottom = 8.dp),
    )
}

@Composable
private fun CenteredMessage(
    text: String,
    body: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        body?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clickable(onClick = onAction)
                    .testTag(TierDetailTestTags.MOVIE_SEARCH_TRY_AGAIN)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun MovieResultsList(
    items: List<CatalogueItem>,
    selectedIds: Set<String>,
    selectedTint: Color,
    onToggle: (CatalogueItem) -> Unit,
) {
    LazyColumn(modifier = Modifier.testTag(TierDetailTestTags.MOVIE_SEARCH_RESULTS_LIST)) {
        items(items, key = { it.id }) { item ->
            val isSelected = item.id in selectedIds
            val background = if (isSelected) selectedTint else Color.Transparent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
                    .selectable(selected = isSelected, onClick = { onToggle(item) })
                    .testTag(TierDetailTestTags.movieSearchResult(item.id))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(width = 44.dp, height = 64.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 16.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    item.subtitle?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = TierYourLifeType.current.captionUnderTitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SelectionCheckbox(isSelected = isSelected)
            }
        }
    }
}

// Hand-drawn rather than the stock M3 Checkbox: the mock calls for an exact 24dp box with a
// 4dp corner radius, which is smaller and squarer than M3's own default checkbox geometry.
@Composable
private fun SelectionCheckbox(isSelected: Boolean) {
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .then(
                if (isSelected) {
                    Modifier.background(primary)
                } else {
                    Modifier.border(2.dp, outline, RoundedCornerShape(4.dp))
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            CheckIcon(18.dp, onPrimary)
        }
    }
}

// Mirrors TierEditorSheet's Cancel/Save row for the disabled/enabled treatment (12%/38%-alpha
// onSurface vs. primary/onPrimary) — no click handler at all when disabled.
@Composable
private fun SelectionBar(selectedCount: Int, fill: Color, onConfirm: () -> Unit) {
    Column(modifier = Modifier.testTag(TierDetailTestTags.MOVIE_SEARCH_BOTTOM_BAR)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(fill)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val countText = if (selectedCount == 0) {
                stringResource(R.string.movie_search_selected_none)
            } else {
                pluralStringResource(R.plurals.movie_search_selected_count, selectedCount, selectedCount)
            }
            Text(
                text = countText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TierDetailTestTags.MOVIE_SEARCH_SELECTED_COUNT),
            )

            val hasSelection = selectedCount > 0
            val containerColor = if (hasSelection) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            }
            val contentColor = if (hasSelection) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
            val buttonLabel = if (hasSelection) {
                pluralStringResource(R.plurals.movie_search_add_button, selectedCount, selectedCount)
            } else {
                stringResource(R.string.movie_search_add_disabled)
            }
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(containerColor)
                    .then(
                        if (hasSelection) {
                            Modifier.clickable(onClick = onConfirm)
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 24.dp)
                    .testTag(TierDetailTestTags.MOVIE_SEARCH_CONFIRM),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = buttonLabel, style = MaterialTheme.typography.bodyMedium, color = contentColor)
            }
        }
    }
}

private val previewMovies = listOf(
    CatalogueItem(id = "tmdb:1", title = "The Godfather", subtitle = "1972", imageUrl = null),
    CatalogueItem(id = "tmdb:2", title = "Inception", subtitle = "2010", imageUrl = null),
)

@Preview(name = "Initial", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun MovieSearchScreenInitialPreview() {
    TierYourLifeTheme {
        MovieSearchScreenContent(
            state = MovieSearchUiState.Initial,
            query = "",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Sci-fi films",
        )
    }
}

@Preview(name = "Loading", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun MovieSearchScreenLoadingPreview() {
    TierYourLifeTheme {
        MovieSearchScreenContent(
            state = MovieSearchUiState.Loading,
            query = "Interstellar",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Sci-fi films",
        )
    }
}

@Preview(name = "Success", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun MovieSearchScreenSuccessPreview() {
    TierYourLifeTheme {
        MovieSearchScreenContent(
            state = MovieSearchUiState.Success(items = previewMovies),
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
fun MovieSearchScreenEmptyPreview() {
    TierYourLifeTheme {
        MovieSearchScreenContent(
            state = MovieSearchUiState.Empty(query = "zzxxccvv"),
            query = "zzxxccvv",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Sci-fi films",
        )
    }
}

@Preview(name = "Error", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun MovieSearchScreenErrorPreview() {
    TierYourLifeTheme {
        MovieSearchScreenContent(
            state = MovieSearchUiState.Error(message = "No connection to server"),
            query = "Interstellar",
            onQueryChange = {},
            onSearchClick = {},
            listTitle = "Sci-fi films",
        )
    }
}
