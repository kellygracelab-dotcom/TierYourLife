package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.aistudio.presentation.R

private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private const val SHIMMER_DURATION_MILLIS = 1200

@Composable
internal fun GeneratingCard(testTag: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .testTag(testTag)
            .padding(12.dp),
    ) {
        ShimmerBlock(
            modifier = Modifier
                .size(width = 192.dp, height = 256.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.ai_generating),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShimmerBlock(modifier: Modifier = Modifier) {
    val highest = MaterialTheme.colorScheme.surfaceContainerHighest
    val high = MaterialTheme.colorScheme.surfaceContainerHigh
    val context = LocalContext.current
    val animationsDisabled = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    if (animationsDisabled) {
        Box(modifier = modifier.background(highest))
        return
    }

    val transition = rememberInfiniteTransition(label = "ai_shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SHIMMER_DURATION_MILLIS, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ai_shimmer_progress",
    )
    Box(modifier = modifier.background(lerp(highest, high, progress)))
}

@Preview(showBackground = true)
@Composable
private fun GeneratingCardLightPreview() = TierYourLifeTheme(false) {
    GeneratingCard(testTag = "generating_card_preview")
}

@Preview(showBackground = true)
@Composable
private fun GeneratingCardDarkPreview() = TierYourLifeTheme(true) {
    GeneratingCard(testTag = "generating_card_preview")
}
