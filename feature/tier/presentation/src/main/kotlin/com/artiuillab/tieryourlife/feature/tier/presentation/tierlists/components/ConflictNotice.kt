package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.VectorIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

/**
 * Two phones changed the same board apart from each other, so both versions
 * are here.
 *
 * Not an error, and coloured accordingly: nothing failed and nothing is lost.
 * The only thing a person has to do is look at the two and decide, which is
 * exactly what the text says.
 */
@Composable
internal fun ConflictBanner(title: String, onGotIt: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth().testTag(TierListsTestTags.CONFLICT_BANNER),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 6.dp)) {
            SplitIcon(22.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    text = stringResource(R.string.conflict_title, title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.conflict_body),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onGotIt,
                        modifier = Modifier.testTag(TierListsTestTags.CONFLICT_GOT_IT),
                    ) {
                        Text(stringResource(R.string.conflict_got_it))
                    }
                }
            }
        }
    }
}

/**
 * Beside the title of the copy that arrived. Deliberately not in the primary
 * colour: it is a fact about where a board came from, not a state anybody has
 * to act on.
 */
@Composable
internal fun ArrivedFromChip(deviceName: String?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.testTag(TierListsTestTags.CONFLICT_TAG),
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Text(
            text = if (deviceName != null) {
                stringResource(R.string.conflict_tag, deviceName)
            } else {
                stringResource(R.string.conflict_tag_unknown)
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SplitIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.7f * scale
    drawLine(color, Offset(12f * scale, 21f * scale), Offset(12f * scale, 13f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(12f * scale, 13f * scale), Offset(6f * scale, 7f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(12f * scale, 13f * scale), Offset(18f * scale, 7f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(3f * scale, 7f * scale), Offset(9f * scale, 7f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(15f * scale, 7f * scale), Offset(21f * scale, 7f * scale), stroke, StrokeCap.Round)
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ConflictBannerPreview() = TierYourLifeTheme(false) {
    ConflictBanner(title = "Sci-fi films", onGotIt = {}, modifier = Modifier.padding(16.dp))
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ArrivedFromChipPreview() = TierYourLifeTheme(true) {
    ArrivedFromChip(deviceName = "Pixel 7", modifier = Modifier.padding(16.dp))
}
