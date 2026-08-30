package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.feature.aistudio.presentation.R
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.AiStudioTestTags

@Composable
internal fun EmptyState(onHintClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val hints = listOf(
        stringResource(R.string.ai_hint_1),
        stringResource(R.string.ai_hint_2),
        stringResource(R.string.ai_hint_3),
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) {
            AutoAwesomeIcon(28.dp, MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.ai_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.ai_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 264.dp),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.ai_hints_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier.atMost(ContentWidth.Reading),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            hints.forEachIndexed { index, hint ->
                HintRow(
                    text = hint,
                    testTag = AiStudioTestTags.hint(index),
                    onClick = { onHintClick(hint) },
                )
            }
        }
    }
}

@Composable
private fun HintRow(text: String, testTag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStateLightPreview() = TierYourLifeTheme(false) {
    EmptyState(onHintClick = {})
}

@Preview(showBackground = true)
@Composable
private fun EmptyStateDarkPreview() = TierYourLifeTheme(true) {
    EmptyState(onHintClick = {})
}
