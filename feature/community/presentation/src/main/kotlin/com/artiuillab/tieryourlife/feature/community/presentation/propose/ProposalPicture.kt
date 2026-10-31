package com.artiuillab.tieryourlife.feature.community.presentation.propose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.artiuillab.tieryourlife.core.theme.ui.PhotoLibraryIcon
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.explore.TileNameStyle
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionLabel
import com.artiuillab.tieryourlife.feature.community.presentation.section.SymbolIcon
import com.artiuillab.tieryourlife.feature.community.presentation.section.TileScrimBottom
import com.artiuillab.tieryourlife.feature.community.presentation.section.TileScrimTop

/** Sharp enough to zoom into, small enough to decode at once. */
private const val PREVIEW_EDGE_PX = 1600

/** Where the picture goes before there is one: the 16:10 space it will fill, asking for it. */
@Composable
internal fun PictureDropZone(onPick: () -> Unit, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outline
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(PICTURE_ASPECT)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onPick)
            .testTag(ProposeTestTags.DROP_ZONE),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 1.5.dp.toPx()
            drawRoundRect(
                color = outline,
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PhotoLibraryIcon(32.dp, outline)
            Text(stringResource(R.string.propose_picture_add), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                stringResource(R.string.propose_picture_hint),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The picture under the fixed 16:10 frame, dragged to place and pinched to
 * zoom, with the tile's own scrim and label over it: the name and icon as
 * they will sit on the home page, and a sample "new this week" line.
 */
@Composable
internal fun PictureCropFrame(
    picture: PickedPicture,
    frame: PictureFrame,
    icon: String,
    name: String,
    onMove: (PictureFrame) -> Unit,
    modifier: Modifier = Modifier,
) {
    val request = ImageRequest.Builder(LocalPlatformContext.current).data(picture.uri).size(PREVIEW_EDGE_PX).build()
    val painter = rememberAsyncImagePainter(request)
    val description = stringResource(R.string.propose_picture_frame)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(PICTURE_ASPECT)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .semantics { contentDescription = description }
            .testTag(ProposeTestTags.CROP_FRAME),
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(picture, widthPx) {
                    var current = frame
                    detectTransformGestures { _, pan, zoom, _ ->
                        current = current.moved(pan.x / widthPx, pan.y / widthPx, zoom, picture.size)
                        onMove(current)
                    }
                },
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = frame.zoom
                        scaleY = frame.zoom
                        translationX = frame.offsetX * widthPx
                        translationY = frame.offsetY * widthPx
                    },
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(TileScrimTop, TileScrimBottom))))
            TileLabelPreview(icon = icon, name = name, modifier = Modifier.align(Alignment.BottomStart))
        }
    }
}

@Composable
private fun TileLabelPreview(icon: String, name: String, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(start = 12.dp, end = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        SymbolIcon(icon, 18.dp, SectionLabel)
        Text(
            name.ifBlank { stringResource(R.string.propose_name_placeholder) },
            style = TileNameStyle,
            color = SectionLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** What the picture area says when it wants another picture, or another crop. */
@Composable
internal fun pictureNoticeText(notice: PictureNotice): String = when (notice) {
    is PictureNotice.TooSmall -> stringResource(R.string.propose_picture_too_small, notice.width)
    PictureNotice.NotCropped -> stringResource(R.string.propose_picture_not_cropped)
    PictureNotice.Unreadable -> stringResource(R.string.propose_picture_unreadable)
}

private const val PICTURE_ASPECT = 1.6f
