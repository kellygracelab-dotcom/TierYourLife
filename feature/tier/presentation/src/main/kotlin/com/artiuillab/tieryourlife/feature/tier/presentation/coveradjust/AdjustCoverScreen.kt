package com.artiuillab.tieryourlife.feature.tier.presentation.coveradjust

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Scale
import com.artiuillab.tieryourlife.core.theme.layout.CenteredContent
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.isShortWindow
import com.artiuillab.tieryourlife.core.theme.layout.systemBarsContentPadding
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.ui.ActionFailedSnackbar
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** The shapes the cover is drawn in most: a feed tile (183 × 132) and a page's header (a phone's width × 200). */
internal const val FEED_TILE_ASPECT = 183f / 132f
internal const val PAGE_HEADER_ASPECT = 411f / 200f

/** The frame the picture is moved under: between the tile and the header, so neither surprises. */
private const val EDITOR_ASPECT = 4f / 3f
private val EditorFrame = Size(EDITOR_ASPECT, 1f)
private val EditorMargin = 20.dp
private val EditorMaxWidth = 480.dp
private val FrameCorner = 12.dp
private const val DIM_ALPHA = 0.5f
private val SIDE_BY_SIDE_FROM = 600.dp
private val SIDE_BY_SIDE_MAX_WIDTH = 1040.dp
private const val EDITOR_SHARE = 0.85f
private const val CONTROLS_SHARE = 1.15f
private val ButtonPadding = PaddingValues(horizontal = 18.dp)

/** Enough for a close crop of a phone photo to stay sharp, not the whole camera file. */
private const val DECODE_PX = 2048

/**
 * After a single picture is chosen, and from Cover → Adjust position: the
 * picture moves under a frame by finger, closer by pinch or the slider, with
 * the feed's tile and a page's header beside it cut the way they will be.
 */
@Composable
fun AdjustCoverScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AdjustCoverViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    LaunchedEffect(state.nothingToAdjust) { if (state.nothingToAdjust) onClose() }
    val context = LocalContext.current
    val request = remember(state.picture) {
        ImageRequest.Builder(context).data(state.picture).size(DECODE_PX).scale(Scale.FIT).build()
    }
    AdjustCoverContent(
        state = state,
        painter = rememberAsyncImagePainter(request),
        onMove = viewModel::move,
        onReset = viewModel::reset,
        onDone = viewModel::save,
        onClose = onClose,
        userMessages = viewModel.userMessages,
    )
}

@Composable
fun AdjustCoverContent(
    state: AdjustCoverUiState,
    painter: Painter,
    onMove: (CoverFocus) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    onClose: () -> Unit,
    userMessages: Flow<UserMessage> = emptyFlow(),
) {
    Surface(Modifier.fillMaxSize().testTag(AdjustCoverTestTags.SCREEN), color = MaterialTheme.colorScheme.surface) {
        Box {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                TopBar(onClose)
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    // Side by side wherever height is the short side: a phone sideways, a Flip's cover, a tablet.
                    if (isShortWindow || (maxWidth >= SIDE_BY_SIDE_FROM && maxWidth > maxHeight)) {
                        SideBySide(state, painter, onMove, onReset, onDone)
                    } else {
                        Stacked(state, painter, onMove, onReset, onDone)
                    }
                }
            }
            ActionFailedSnackbar(userMessages)
        }
    }
}

@Composable
private fun SideBySide(
    state: AdjustCoverUiState,
    painter: Painter,
    onMove: (CoverFocus) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier
                .fillMaxSize()
                .widthIn(max = SIDE_BY_SIDE_MAX_WIDTH)
                .padding(systemBarsContentPadding(start = 16.dp, end = 16.dp, bottom = 12.dp)),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CoverEditor(painter, state.focus, onMove, Modifier.weight(EDITOR_SHARE).fillMaxSize())
            Column(
                Modifier.weight(CONTROLS_SHARE).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Controls(state, painter, onMove, onReset, onDone)
            }
        }
    }
}

@Composable
private fun Stacked(
    state: AdjustCoverUiState,
    painter: Painter,
    onMove: (CoverFocus) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(systemBarsContentPadding(bottom = 24.dp)),
    ) {
        CenteredContent(ContentWidth.Reading) {
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CoverEditor(painter, state.focus, onMove, Modifier.width(EditorMaxWidth + EditorMargin * 2))
                Controls(state, painter, onMove, onReset, onDone)
            }
        }
    }
}

@Composable
private fun TopBar(onClose: () -> Unit) {
    val closeDescription = stringResource(R.string.cover_adjust_close)
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = closeDescription }
                .testTag(AdjustCoverTestTags.CLOSE),
        ) { CloseMark(24.dp, MaterialTheme.colorScheme.onSurface) }
        Text(
            text = stringResource(R.string.cover_adjust_title),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Controls(
    state: AdjustCoverUiState,
    painter: Painter,
    onMove: (CoverFocus) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
) {
    val focus = state.focus
    Text(
        text = stringResource(R.string.cover_adjust_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.cover_adjust_zoom),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 12.dp),
        )
        Slider(
            value = focus.zoom,
            onValueChange = { zoom -> onMove(focus.moved(Offset.Zero, zoom / focus.zoom, painter.intrinsicSize, EditorFrame)) },
            valueRange = CoverFocus.MIN_ZOOM..CoverFocus.MAX_ZOOM,
            modifier = Modifier.weight(1f).testTag(AdjustCoverTestTags.ZOOM),
        )
    }
    Text(
        text = stringResource(R.string.cover_adjust_preview),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
        Preview(
            painter = painter,
            focus = focus,
            aspect = FEED_TILE_ASPECT,
            label = stringResource(R.string.cover_adjust_feed),
            modifier = Modifier.weight(FEED_TILE_ASPECT).testTag(AdjustCoverTestTags.FEED_PREVIEW),
        )
        Preview(
            painter = painter,
            focus = focus,
            aspect = PAGE_HEADER_ASPECT,
            label = stringResource(R.string.cover_adjust_header),
            modifier = Modifier.weight(PAGE_HEADER_ASPECT).testTag(AdjustCoverTestTags.HEADER_PREVIEW),
        )
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        OutlinedButton(
            onClick = onReset,
            enabled = !focus.isCentred,
            contentPadding = ButtonPadding,
            modifier = Modifier.height(40.dp).testTag(AdjustCoverTestTags.RESET),
        ) { Text(stringResource(R.string.cover_picker_reset), maxLines = 1) }
        Button(
            onClick = onDone,
            enabled = !state.saving && state.picture != null,
            contentPadding = ButtonPadding,
            modifier = Modifier.height(40.dp).testTag(AdjustCoverTestTags.DONE),
        ) { Text(stringResource(R.string.cover_picker_done), maxLines = 1) }
    }
}

/**
 * The picture under a 4:3 frame with the rest of it dimmed around: the frame
 * is what the formula shows at this shape, the rest is where it can go.
 */
@Composable
private fun CoverEditor(painter: Painter, focus: CoverFocus, onMove: (CoverFocus) -> Unit, modifier: Modifier) {
    val current by rememberUpdatedState(focus)
    val move by rememberUpdatedState(onMove)
    val dim = Color.Black.copy(alpha = DIM_ALPHA)
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val byWidth = maxWidth - EditorMargin * 2
        val byHeight = if (constraints.hasBoundedHeight) (maxHeight - EditorMargin * 2) * EDITOR_ASPECT else byWidth
        val frameWidth: Dp = min(byWidth, byHeight).coerceAtLeast(0.dp)
        Canvas(
            Modifier
                .size(frameWidth + EditorMargin * 2, frameWidth / EDITOR_ASPECT + EditorMargin * 2)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .testTag(AdjustCoverTestTags.EDITOR)
                .pointerInput(painter) {
                    val margin = EditorMargin.toPx() * 2
                    detectTransformGestures { _, pan, zoom, _ ->
                        val frame = Size(size.width - margin, size.height - margin)
                        move(current.moved(pan, zoom, painter.intrinsicSize, frame))
                    }
                },
        ) {
            val margin = EditorMargin.toPx()
            val frame = Rect(margin, margin, size.width - margin, size.height - margin)
            val corner = CornerRadius(FrameCorner.toPx())
            drawCover(painter, focus, frame)
            clipPath(Path().apply { addRoundRect(RoundRect(frame, corner)) }, ClipOp.Difference) { drawRect(dim) }
            drawRoundRect(Color.White, frame.topLeft, frame.size, corner, style = Stroke(2.dp.toPx()))
        }
    }
}

@Composable
private fun Preview(painter: Painter, focus: CoverFocus, aspect: Float, label: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) { drawCover(painter, focus, Rect(Offset.Zero, size)) }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CloseMark(iconSize: Dp, color: Color) = Canvas(Modifier.size(iconSize)) {
    val unit = size.minDimension / 24f
    drawLine(color, Offset(6f * unit, 6f * unit), Offset(18f * unit, 18f * unit), 1.8f * unit, StrokeCap.Round)
    drawLine(color, Offset(18f * unit, 6f * unit), Offset(6f * unit, 18f * unit), 1.8f * unit, StrokeCap.Round)
}
