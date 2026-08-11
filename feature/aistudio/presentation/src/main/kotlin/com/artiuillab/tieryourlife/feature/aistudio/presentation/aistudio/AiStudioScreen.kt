package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.aistudio.presentation.R
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components.AiComposer
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components.BackIcon
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components.EmptyState
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components.ErrorCard
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components.GeneratingCard
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components.NamingDialog
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components.PromptBubble
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components.ResultCard
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components.previewAiStudioConversationState
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components.previewAiStudioEmptyState
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components.previewAiStudioFailedState
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components.previewAiStudioGeneratingState

@Composable
fun AiStudioScreen(
    onBack: () -> Unit,
    onCardAdded: (Long) -> Unit,
    viewModel: AiStudioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var fieldText by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    AiStudioScreenContent(
        state = state,
        listTitle = viewModel.listTitle,
        fieldText = fieldText,
        onFieldTextChange = { fieldText = it },
        onBack = onBack,
        onSend = {
            viewModel.send(fieldText)
            fieldText = ""
        },
        onHintClick = { hint -> fieldText = hint },
        onRetry = viewModel::retry,
        onRegenerate = viewModel::regenerate,
        onSaveCard = { exchangeId, title ->
            viewModel.addToList(exchangeId, title, onAdded = onCardAdded)
        },
        fieldFocusRequester = focusRequester,
    )
}

@Composable
internal fun AiStudioScreenContent(
    state: AiStudioUiState,
    listTitle: String,
    fieldText: String,
    onFieldTextChange: (String) -> Unit,
    onBack: () -> Unit,
    onSend: () -> Unit,
    onHintClick: (String) -> Unit,
    onRetry: (Long) -> Unit,
    onRegenerate: (Long) -> Unit,
    onSaveCard: (exchangeId: Long, title: String) -> Unit,
    modifier: Modifier = Modifier,
    fieldFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    var namingExchangeId by remember { mutableStateOf<Long?>(null) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxSize()
                .testTag(AiStudioTestTags.SCREEN),
        ) {
            AiStudioTopBar(onBack = onBack)
            Text(
                text = String.format(stringResource(R.string.ai_caption), listTitle),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(modifier = Modifier.weight(1f)) {
                if (state.exchanges.isEmpty()) {
                    EmptyState(
                        onHintClick = { hint ->
                            onHintClick(hint)
                            fieldFocusRequester.requestFocus()
                        },
                    )
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(state.exchanges.size) {
                        listState.animateScrollToItem(0)
                    }
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.exchanges.asReversed(), key = { it.id }) { exchange ->
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                PromptBubble(
                                    prompt = exchange.prompt,
                                    testTag = AiStudioTestTags.bubble(exchange.id),
                                )
                                when (val phase = exchange.phase) {
                                    AiExchangePhase.Generating -> GeneratingCard(
                                        testTag = AiStudioTestTags.result(exchange.id),
                                    )

                                    is AiExchangePhase.Result -> ResultCard(
                                        image = phase.image,
                                        testTag = AiStudioTestTags.result(exchange.id),
                                        onAddToList = { namingExchangeId = exchange.id },
                                        onRegenerate = { onRegenerate(exchange.id) },
                                    )

                                    AiExchangePhase.Failed -> ErrorCard(
                                        testTag = AiStudioTestTags.error(exchange.id),
                                        onTryAgain = { onRetry(exchange.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AiComposer(
                text = fieldText,
                onTextChange = onFieldTextChange,
                generating = state.generating,
                onSend = onSend,
                focusRequester = fieldFocusRequester,
            )
        }
    }

    val namingId = namingExchangeId
    val namingImage = namingId?.let { id ->
        (state.exchanges.firstOrNull { it.id == id }?.phase as? AiExchangePhase.Result)?.image
    }
    if (namingId != null && namingImage != null) {
        NamingDialog(
            imageUri = namingImage.imageUri,
            onDismiss = { namingExchangeId = null },
            onSave = { title ->
                onSaveCard(namingId, title)
                namingExchangeId = null
            },
        )
    }
}

@Composable
private fun AiStudioTopBar(onBack: () -> Unit) {
    val backDescription = stringResource(R.string.cd_back)
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
        ) {
            BackIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = stringResource(R.string.ai_title),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@TierYourLifeDevicePreviews
@Composable
private fun AiStudioScreenLightPreview() = TierYourLifeTheme(false) {
    AiStudioScreenContent(
        state = previewAiStudioConversationState,
        listTitle = "Sci-fi films",
        fieldText = "",
        onFieldTextChange = {},
        onBack = {},
        onSend = {},
        onHintClick = {},
        onRetry = {},
        onRegenerate = {},
        onSaveCard = { _, _ -> },
    )
}

@TierYourLifeDevicePreviews
@Composable
private fun AiStudioScreenDarkPreview() = TierYourLifeTheme(true) {
    AiStudioScreenContent(
        state = previewAiStudioConversationState,
        listTitle = "Sci-fi films",
        fieldText = "",
        onFieldTextChange = {},
        onBack = {},
        onSend = {},
        onHintClick = {},
        onRetry = {},
        onRegenerate = {},
        onSaveCard = { _, _ -> },
    )
}

@Preview(name = "Empty", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
private fun AiStudioScreenEmptyPreview() = TierYourLifeTheme {
    AiStudioScreenContent(
        state = previewAiStudioEmptyState,
        listTitle = "Sci-fi films",
        fieldText = "",
        onFieldTextChange = {},
        onBack = {},
        onSend = {},
        onHintClick = {},
        onRetry = {},
        onRegenerate = {},
        onSaveCard = { _, _ -> },
    )
}

@Preview(name = "Generating", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
private fun AiStudioScreenGeneratingPreview() = TierYourLifeTheme {
    AiStudioScreenContent(
        state = previewAiStudioGeneratingState,
        listTitle = "Sci-fi films",
        fieldText = "",
        onFieldTextChange = {},
        onBack = {},
        onSend = {},
        onHintClick = {},
        onRetry = {},
        onRegenerate = {},
        onSaveCard = { _, _ -> },
    )
}

@Preview(name = "Error", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
private fun AiStudioScreenErrorPreview() = TierYourLifeTheme {
    AiStudioScreenContent(
        state = previewAiStudioFailedState,
        listTitle = "Sci-fi films",
        fieldText = "",
        onFieldTextChange = {},
        onBack = {},
        onSend = {},
        onHintClick = {},
        onRetry = {},
        onRegenerate = {},
        onSaveCard = { _, _ -> },
    )
}
