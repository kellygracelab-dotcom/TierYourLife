package com.artiuillab.tieryourlife.feature.account.presentation.account.face

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import com.artiuillab.tieryourlife.feature.account.presentation.R
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountTestTags
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

/** Sharp enough to zoom into a face, small enough to decode at once. */
private const val PREVIEW_EDGE_PX = 1600
private const val SCRIM_ALPHA = 0.55f
private val FRAME_MAX = 420.dp
private val FRAME_MARGIN = 24.dp
private val SIDE_CONTROLS_WIDTH = 220.dp

/**
 * The chosen picture behind a square frame with the circle people will see:
 * dragged to place, pinched to zoom. Beside the frame when the window is
 * wider than tall (a phone on its side, the Flip's cover), under it otherwise.
 * Confirms null when nothing was moved: the middle square needs no cutting.
 */
@Composable
internal fun FacePositioner(source: String, onCancel: () -> Unit, onConfirm: (FaceCrop?) -> Unit) {
    var frame by rememberSaveable(source, stateSaver = FaceFrame.Saver) { mutableStateOf(FaceFrame()) }
    val request = ImageRequest.Builder(LocalPlatformContext.current).data(source).size(PREVIEW_EDGE_PX).build()
    val painter = rememberAsyncImagePainter(request)
    val loading by painter.state.collectAsState()
    val loaded = loading as? AsyncImagePainter.State.Success
    val aspect = loaded?.painter?.intrinsicSize?.let { size ->
        if (size.width > 0f && size.height > 0f) size.width / size.height else null
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().testTag(AccountTestTags.FACE_POSITIONER), color = MaterialTheme.colorScheme.surface) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
                val width = maxWidth
                val height = maxHeight
                val wide = width > height
                val controls = @Composable {
                    Controls(
                        failed = loading is AsyncImagePainter.State.Error,
                        ready = aspect != null || loading is AsyncImagePainter.State.Error,
                        onCancel = onCancel,
                        onConfirm = { onConfirm(aspect?.let { frame.takeUnless { it.untouched }?.crop(it) }) },
                    )
                }
                if (wide) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        val side = frameSide(width - SIDE_CONTROLS_WIDTH, height)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Frame(painter, aspect, frame, side) { frame = it }
                        }
                        Box(Modifier.width(SIDE_CONTROLS_WIDTH).padding(end = 16.dp)) { controls() }
                    }
                } else {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Frame(painter, aspect, frame, frameSide(width, height * 2 / 3)) { frame = it }
                        }
                        Box(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) { controls() }
                    }
                }
            }
        }
    }
}

private fun frameSide(width: Dp, height: Dp): Dp = (minOf(width, height) - FRAME_MARGIN * 2).coerceIn(120.dp, FRAME_MAX)

@Composable
private fun Frame(
    painter: AsyncImagePainter,
    aspect: Float?,
    frame: FaceFrame,
    side: Dp,
    onMove: (FaceFrame) -> Unit,
) {
    val sidePx = with(LocalDensity.current) { side.toPx() }
    val description = stringResource(R.string.account_face_position_frame)
    val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
    val ring = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .size(side)
            .clipToBounds()
            .semantics { contentDescription = description }
            .pointerInput(aspect, sidePx) {
                if (aspect == null) return@pointerInput
                var current = frame
                detectTransformGestures { _, pan, zoom, _ ->
                    current = current.moved(pan.x / sidePx, pan.y / sidePx, zoom, aspect)
                    onMove(current)
                }
            },
        contentAlignment = Alignment.Center,
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
                    translationX = frame.offsetX * sidePx
                    translationY = frame.offsetY * sidePx
                },
        )
        if (aspect == null) CircularProgressIndicator()
        Canvas(Modifier.fillMaxSize()) {
            val outside = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                addOval(Rect(Offset.Zero, size))
            }
            drawPath(outside, scrim)
            drawCircle(ring, radius = size.minDimension / 2f - 1.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
private fun Controls(failed: Boolean, ready: Boolean, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.account_face_position_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(if (failed) R.string.account_face_position_failed else R.string.account_face_position_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.testTag(AccountTestTags.FACE_POSITION_CANCEL)) {
                Text(stringResource(ThemeR.string.action_cancel))
            }
            Button(onClick = onConfirm, enabled = ready, modifier = Modifier.testTag(AccountTestTags.FACE_POSITION_CONFIRM)) {
                Text(stringResource(R.string.account_face_position_use))
            }
        }
    }
}
