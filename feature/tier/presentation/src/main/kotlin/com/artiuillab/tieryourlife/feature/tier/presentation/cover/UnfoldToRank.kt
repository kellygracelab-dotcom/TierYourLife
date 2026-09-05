package com.artiuillab.tieryourlife.feature.tier.presentation.cover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.VectorIcon
import kotlinx.coroutines.delay

/**
 * What the cover says when somebody reaches for something it cannot do. No
 * buttons: unfolding is the action. Leaves on its own, because a notice that
 * waits to be dismissed is the opposite of glancing.
 */
@Composable
internal fun UnfoldToRank(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) {
        delay(VISIBLE_MILLIS)
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = SCRIM_ALPHA))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .testTag(CoverTestTags.UNFOLD),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                ExpandIcon(26.dp, MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                text = stringResource(R.string.cover_unfold_title),
                modifier = Modifier.padding(top = 14.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.cover_unfold_body),
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ExpandIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    fun at(x: Float, y: Float) = Offset(x * scale, y * scale)
    val stroke = 2f * scale
    // Two corners and the arrow between them: out of the middle, into both.
    drawLine(color, at(10f, 14f), at(5f, 19f), stroke, StrokeCap.Round)
    drawLine(color, at(5f, 19f), at(5f, 14f), stroke, StrokeCap.Round)
    drawLine(color, at(5f, 19f), at(10f, 19f), stroke, StrokeCap.Round)
    drawLine(color, at(14f, 10f), at(19f, 5f), stroke, StrokeCap.Round)
    drawLine(color, at(19f, 5f), at(19f, 10f), stroke, StrokeCap.Round)
    drawLine(color, at(19f, 5f), at(14f, 5f), stroke, StrokeCap.Round)
}

/** Long enough to read twice, short enough not to be in the way. */
private const val VISIBLE_MILLIS = 4_000L

/** The board is still the subject; this is a note over it. */
private const val SCRIM_ALPHA = 0.78f

@androidx.compose.ui.tooling.preview.Preview(
    name = "Flip cover",
    device = "spec:width=352dp,height=339dp,dpi=340",
    showSystemUi = false,
)
@Composable
private fun UnfoldToRankPreview() = TierYourLifeTheme(true) {
    UnfoldToRank(onDismiss = {})
}
