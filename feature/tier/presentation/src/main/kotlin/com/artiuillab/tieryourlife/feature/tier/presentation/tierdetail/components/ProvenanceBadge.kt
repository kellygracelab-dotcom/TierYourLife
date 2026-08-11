package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.VectorIcon
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun GeneratedBadge(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_ai_badge)
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        AutoAwesomeIcon(10.dp, MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
internal fun AutoAwesomeIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawSparkle(color, scale)
}

internal fun DrawScope.drawSparkle(
    color: Color,
    scale: Float,
    centerX: Float = 12f,
    centerY: Float = 12f,
    outerRadius: Float = 9f,
    innerRadius: Float = 3.6f,
) {
    val path = Path()
    for (index in 0 until 8) {
        val angleDegrees = -90.0 + index * 45.0
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val angleRadians = Math.toRadians(angleDegrees)
        val x = (centerX + radius * cos(angleRadians)).toFloat() * scale
        val y = (centerY + radius * sin(angleRadians)).toFloat() * scale
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
