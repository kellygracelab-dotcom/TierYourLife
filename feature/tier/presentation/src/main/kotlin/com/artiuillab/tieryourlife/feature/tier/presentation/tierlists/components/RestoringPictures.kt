package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PictureRestore
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.VectorIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

/**
 * A new phone: boards arrived in one request, pictures take as long as they
 * take. Shown only while something is coming down; the count is the only
 * thing that says the holes are temporary.
 */
@Composable
internal fun RestoringPictures(progress: PictureRestore.Progress, modifier: Modifier = Modifier) {
    if (progress.finished) return

    val fraction by animateFloatAsState(
        targetValue = progress.done.toFloat() / progress.total,
        label = "restoringPictures",
    )

    Column(modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CloudDownloadIcon(18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                // The singular shows no number ("1 of 1" reads as a joke). Two
                // strings rather than a plural: in French "one" also covers zero.
                text = if (progress.total == 1) {
                    stringResource(R.string.restoring_one_picture)
                } else {
                    stringResource(R.string.restoring_pictures, progress.done, progress.total)
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .testTag(TierListsTestTags.RESTORING_PICTURES),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CloudDownloadIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = Stroke(1.7f * scale)
    drawCircle(color, 3.6f * scale, Offset(9f * scale, 10f * scale), style = stroke)
    drawCircle(color, 4.6f * scale, Offset(15f * scale, 9.5f * scale), style = stroke)
    drawLine(
        color,
        Offset(12f * scale, 13f * scale),
        Offset(12f * scale, 20f * scale),
        1.7f * scale,
        StrokeCap.Round,
    )
    drawLine(color, Offset(9f * scale, 17f * scale), Offset(12f * scale, 20f * scale), 1.7f * scale, StrokeCap.Round)
    drawLine(color, Offset(15f * scale, 17f * scale), Offset(12f * scale, 20f * scale), 1.7f * scale, StrokeCap.Round)
}

@Preview(showBackground = true)
@Composable
private fun RestoringPicturesPreview() = TierYourLifeTheme(false) {
    RestoringPictures(PictureRestore.Progress(done = 12, total = 34))
}

@Preview(showBackground = true)
@Composable
private fun RestoringOnePictureDarkPreview() = TierYourLifeTheme(true) {
    RestoringPictures(PictureRestore.Progress(done = 0, total = 1))
}
