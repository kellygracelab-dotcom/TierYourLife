package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.settings.HiddenEntry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.SectionLabel
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.HideIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon

private val SectionLabelPadding =
    Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)

@Composable
fun HiddenScreen(
    onBack: () -> Unit,
    viewModel: HiddenViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HiddenScreenContent(
        state = state,
        onBack = onBack,
        onShowListAgain = viewModel::showListAgain,
        onShowAuthorAgain = viewModel::showAuthorAgain,
    )
}

@Composable
internal fun HiddenScreenContent(
    state: HiddenUiState,
    onBack: () -> Unit,
    onShowListAgain: (String) -> Unit = {},
    onShowAuthorAgain: (String) -> Unit = {},
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().testTag(HiddenTestTags.SCREEN)) {
            TopBar(onBack = onBack)

            if (state.isEmpty) {
                EmptyState()
                return@Column
            }

            Text(
                text = stringResource(R.string.hidden_subtitle),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
            ) {
                if (state.lists.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.hidden_lists), SectionLabelPadding)
                    state.lists.forEach { entry ->
                        HiddenRow(entry, R.string.hidden_unnamed_list, HiddenTestTags.listRow(entry.id)) {
                            onShowListAgain(entry.id)
                        }
                    }
                }
                if (state.people.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.hidden_people), SectionLabelPadding)
                    state.people.forEach { entry ->
                        HiddenRow(entry, R.string.hidden_unnamed_person, HiddenTestTags.personRow(entry.id)) {
                            onShowAuthorAgain(entry.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val backDescription = stringResource(R.string.tier_detail_content_description_back)
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
                .semantics { contentDescription = backDescription }
                .testTag(HiddenTestTags.BACK),
        ) { BackIcon() }
        Text(
            text = stringResource(R.string.hidden_title),
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun HiddenRow(entry: HiddenEntry, unnamed: Int, testTag: String, onShowAgain: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = entry.label.ifBlank { stringResource(unnamed) },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onShowAgain) { Text(stringResource(R.string.action_show_again)) }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().testTag(HiddenTestTags.EMPTY_STATE),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 264.dp).padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                contentAlignment = Alignment.Center,
            ) { HideIcon(28.dp, MaterialTheme.colorScheme.outline) }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.hidden_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.hidden_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

internal object HiddenTestTags {
    const val SCREEN = "hidden_screen"
    const val BACK = "hidden_back"
    const val EMPTY_STATE = "hidden_empty_state"

    fun listRow(id: String): String = "hidden_list_$id"

    fun personRow(id: String): String = "hidden_person_$id"
}

@TierYourLifeDevicePreviews
@Composable
private fun HiddenScreenPreview() = TierYourLifeTheme {
    HiddenScreenContent(
        state = HiddenUiState(
            lists = listOf(HiddenEntry("1", "Every A24 film, ranked"), HiddenEntry("2", "Ramen places in Kyiv")),
            people = listOf(HiddenEntry("u1", "Danylo Kovalenko")),
        ),
        onBack = {},
    )
}

@TierYourLifeDevicePreviews
@Composable
private fun HiddenScreenEmptyPreview() = TierYourLifeTheme {
    HiddenScreenContent(state = HiddenUiState(), onBack = {})
}
