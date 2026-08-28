package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.PlusIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

private val BOARD_WIDTH = 232.dp
private val ROW_HEIGHT = 26.dp
private const val TILES_PER_ROW = 4

private val TIER_LIGHT = listOf(
    Color(0xFFB03A32),
    Color(0xFFC06A25),
    Color(0xFFA98B1F),
    Color(0xFF3F7F55),
    Color(0xFF3C6E99),
)

private val TIER_DARK = listOf(
    Color(0xFFF1948C),
    Color(0xFFE9A867),
    Color(0xFFD8C05A),
    Color(0xFF7FC393),
    Color(0xFF86B8DE),
)

private val TIER_LABELS = listOf("S", "A", "B", "C", "D")

private val SUGGESTIONS = listOf(
    R.string.home_suggestion_films,
    R.string.home_suggestion_games,
    R.string.home_suggestion_restaurants,
    R.string.home_suggestion_albums,
    R.string.home_suggestion_books,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeEmptyState(onCreateNamedList: (String) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        // Centred while it fits, scrollable once a large font or a short screen
        // makes it taller than the window.
        val windowHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = windowHeight)
                .padding(horizontal = 32.dp)
                .padding(bottom = 96.dp)
                .testTag(TierListsTestTags.EMPTY_STATE),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MiniBoard()
            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.home_start_with),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SUGGESTIONS.forEachIndexed { index, suggestion ->
                    val label = stringResource(suggestion)
                    AssistChip(
                        onClick = { onCreateNamedList(label) },
                        label = { Text(label) },
                        leadingIcon = { PlusIcon(18.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.testTag(TierListsTestTags.suggestion(index)),
                        border = AssistChipDefaults.assistChipBorder(enabled = true),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.home_start_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MiniBoard() {
    val colors = if (MaterialTheme.colorScheme.surface.isDark()) TIER_DARK else TIER_LIGHT
    Column(
        modifier = Modifier
            .width(BOARD_WIDTH)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TIER_LABELS.forEachIndexed { index, label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .size(width = 26.dp, height = ROW_HEIGHT)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors[index]),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                }
                repeat(TILES_PER_ROW) {
                    Spacer(
                        Modifier
                            .size(width = 34.dp, height = ROW_HEIGHT)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    )
                }
            }
        }
    }
}

private fun Color.isDark(): Boolean = (red * 0.299f + green * 0.587f + blue * 0.114f) < 0.5f

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun HomeEmptyStateLightPreview() = TierYourLifeTheme(false) {
    HomeEmptyState(onCreateNamedList = {})
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun HomeEmptyStateDarkPreview() = TierYourLifeTheme(true) {
    HomeEmptyState(onCreateNamedList = {})
}
