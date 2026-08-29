package com.artiuillab.tieryourlife.feature.tier.presentation.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailUiState

private val SAVE_BAR_MIN_HEIGHT = 72.dp
private val SAVE_BUTTON_MAX_WIDTH = 180.dp

@Composable
fun CommunityListScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: CommunityListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CommunityListScreenContent(
        state = state,
        onBack = onBack,
        onMoveItem = viewModel::moveItem,
        onSave = { viewModel.saveToMyLists(onSaved) },
        onRetry = viewModel::load,
    )
}

@Composable
internal fun CommunityListScreenContent(
    state: CommunityListUiState,
    onBack: () -> Unit,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        when (state) {
            CommunityListUiState.Loading -> TierDetailScreenContent(
                state = TierDetailUiState.Loading,
                actions = TierDetailActions(onBack = onBack),
                readOnly = true,
            )

            CommunityListUiState.Error -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .testTag(CommunityTestTags.ERROR),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.community_open_failed),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onRetry) { Text(stringResource(R.string.action_try_again)) }
            }

            is CommunityListUiState.Success -> Column(
                Modifier
                    .fillMaxSize()
                    .testTag(CommunityTestTags.SCREEN),
            ) {
                Box(Modifier.weight(1f)) {
                    TierDetailScreenContent(
                        state = TierDetailUiState.Success(state.list),
                        actions = TierDetailActions(onBack = onBack, onMoveItem = onMoveItem),
                        readOnly = true,
                        subtitle = stringResource(R.string.community_by_author, state.authorName),
                    )
                }
                SaveBar(arranged = state.arranged, saving = state.saving, onSave = onSave)
            }
        }
    }
}

@Composable
private fun SaveBar(arranged: Boolean, saving: Boolean, onSave: () -> Unit) {
    // Both halves are allowed two lines and the bar grows to fit: "Save to my
    // lists" is half again as long in German and nearly twice in French, and a
    // fixed-height row spent that on the sentence beside it.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .navigationBarsPadding()
            .heightIn(min = SAVE_BAR_MIN_HEIGHT)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                if (arranged) R.string.community_not_saved_yet else R.string.community_someone_elses,
            ),
            modifier = Modifier
                .weight(1f)
                .testTag(CommunityTestTags.STATUS),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(12.dp))
        Button(
            onClick = onSave,
            enabled = !saving,
            modifier = Modifier
                .widthIn(max = SAVE_BUTTON_MAX_WIDTH)
                .testTag(CommunityTestTags.SAVE),
        ) {
            Text(
                text = stringResource(R.string.community_save_to_my_lists),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

private val previewList = TierList(
    id = 0,
    title = "Every A24 film",
    tiers = listOf(
        Tier(id = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = emptyList()),
        Tier(
            id = -1,
            label = "Unranked",
            colorLight = "#DAD7E0",
            colorDark = "#46464F",
            isPool = true,
            items = listOf(TierItem(1, "Hereditary", null), TierItem(2, "Moonlight", null)),
        ),
    ),
    authorName = "Danylo K.",
)

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_9")
@Composable
private fun CommunityListPreview() = TierYourLifeTheme {
    CommunityListScreenContent(
        state = CommunityListUiState.Success(previewList, "Danylo K."),
        onBack = {},
        onMoveItem = { _, _, _ -> },
        onSave = {},
        onRetry = {},
    )
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_9")
@Composable
private fun CommunityListArrangedDarkPreview() = TierYourLifeTheme(true) {
    CommunityListScreenContent(
        state = CommunityListUiState.Success(previewList, "Danylo K.", arranged = true),
        onBack = {},
        onMoveItem = { _, _, _ -> },
        onSave = {},
        onRetry = {},
    )
}
