package com.artiuillab.tieryourlife.core.theme.tips

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.R
import kotlin.math.roundToInt

/** A tip to show, in words the screen chose. */
data class ShownTip(val tip: Tip, val text: AnnotatedString)

object TipTestTags {
    const val BUBBLE = "tip_bubble"
    const val GOT_IT = "tip_got_it"
    const val NO_MORE = "tip_no_more"
    const val RING = "tip_ring"
}

/**
 * Where the anchors of [content] are known, and over it, the one tip being shown: a ring on what
 * it is about and a bubble beside it. Nothing is dimmed and nothing is taken from the screen: a
 * touch anywhere else goes where it was going and closes the tip on its way ([onClose]). Without
 * its anchor on screen, the tip waits.
 */
@Composable
fun TipHost(
    shown: ShownTip?,
    onClose: () -> Unit,
    onNoMore: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val anchors = remember { TipAnchors() }
    var host by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var bubble by remember { mutableStateOf<Rect?>(null) }
    val showing by rememberUpdatedState(shown?.takeIf { anchors.of(it.tip) != null })
    val close by rememberUpdatedState(onClose)

    Box(
        modifier
            .onGloballyPositioned { host = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val at = host?.takeIf { it.isAttached }?.positionInRoot()?.plus(down.position)
                    if (showing != null && at != null && bubble?.contains(at) != true) close()
                }
            },
    ) {
        CompositionLocalProvider(LocalTipAnchors provides anchors) { content() }
        val tip = showing
        val anchor = tip?.let { anchors.of(it.tip) }
        val origin = host?.takeIf { it.isAttached }?.positionInRoot()
        if (tip != null && anchor != null && origin != null) {
            BackHandler(onBack = onClose)
            TipOverlay(
                text = tip.text,
                anchor = anchor.bounds.translate(-origin),
                beside = anchor.beside,
                onClose = onClose,
                onNoMore = onNoMore,
                onBubblePlaced = { bubble = it },
            )
        } else {
            bubble = null
        }
    }
}

/** Marks what [tip] is about. Outside a [TipHost] it does nothing. */
fun Modifier.tipAnchor(tip: Tip, beside: Boolean = false): Modifier = this then TipAnchorElement(tip, beside)

@Composable
private fun TipOverlay(
    text: AnnotatedString,
    anchor: Rect,
    beside: Boolean,
    onClose: () -> Unit,
    onNoMore: () -> Unit,
    onBubblePlaced: (Rect) -> Unit,
) {
    val ring = MaterialTheme.colorScheme.primary
    val sheet = MaterialTheme.colorScheme.inverseSurface
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Box(
                Modifier
                    .layoutId(RING)
                    .testTag(TipTestTags.RING)
                    .border(4.dp, ring.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(2.dp)
                    .border(2.dp, ring, RoundedCornerShape(10.dp)),
            )
            Box(
                Modifier
                    .layoutId(ARROW)
                    .rotate(45f)
                    .background(sheet),
            )
            Bubble(
                text = text,
                onClose = onClose,
                onNoMore = onNoMore,
                modifier = Modifier
                    .layoutId(BUBBLE)
                    .onGloballyPositioned { onBubblePlaced(it.boundsInRoot()) },
            )
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val gutter = 12.dp.roundToPx()
        val gap = 10.dp.roundToPx()
        val halo = 4.dp.roundToPx()
        val arrowSize = 14.dp.roundToPx()

        val ringPlaceable = measurables.first { it.layoutId == RING }.measure(
            Constraints.fixed(anchor.width.roundToInt() + 2 * halo, anchor.height.roundToInt() + 2 * halo),
        )
        val arrowPlaceable = measurables.first { it.layoutId == ARROW }.measure(Constraints.fixed(arrowSize, arrowSize))
        val bubbleWidth = minOf(BUBBLE_MAX.roundToPx(), width - 2 * gutter).coerceAtLeast(0)
        val bubblePlaceable = measurables.first { it.layoutId == BUBBLE }.measure(Constraints(maxWidth = bubbleWidth))
        val w = bubblePlaceable.width
        val h = bubblePlaceable.height

        layout(width, height) {
            ringPlaceable.place(anchor.left.roundToInt() - halo, anchor.top.roundToInt() - halo)
            val placement = placeBubble(anchor, beside, w, h, width, height, gutter, gap + halo)
            // The arrow first, so the bubble covers all of it but its point.
            val half = arrowSize / 2
            val arrow = when (placement.side) {
                Side.Above -> Offset(anchor.center.x.coerceIn(placement.x + gutter + half.toFloat(), placement.x + w - gutter - half.toFloat()) - half, (placement.y + h - half).toFloat())
                Side.Below -> Offset(anchor.center.x.coerceIn(placement.x + gutter + half.toFloat(), placement.x + w - gutter - half.toFloat()) - half, (placement.y - half).toFloat())
                Side.Start -> Offset((placement.x + w - half).toFloat(), anchor.center.y.coerceIn(placement.y + gutter + half.toFloat(), placement.y + h - gutter - half.toFloat()) - half)
                Side.End -> Offset((placement.x - half).toFloat(), anchor.center.y.coerceIn(placement.y + gutter + half.toFloat(), placement.y + h - gutter - half.toFloat()) - half)
            }
            arrowPlaceable.place(arrow.x.roundToInt(), arrow.y.roundToInt())
            bubblePlaceable.place(placement.x, placement.y)
        }
    }
}

private enum class Side { Above, Below, Start, End }

private data class Placement(val x: Int, val y: Int, val side: Side)

/**
 * Above the anchor when there is room, else below it; beside a column of cards, to the side of it
 * that faces the board, wherever the column stands.
 */
private fun placeBubble(anchor: Rect, beside: Boolean, w: Int, h: Int, width: Int, height: Int, gutter: Int, gap: Int): Placement {
    fun clampX(x: Float) = x.roundToInt().coerceIn(gutter, (width - gutter - w).coerceAtLeast(gutter))
    fun clampY(y: Float) = y.roundToInt().coerceIn(gutter, (height - gutter - h).coerceAtLeast(gutter))
    if (beside) {
        val toStart = anchor.center.x > width / 2f
        val x = if (toStart) anchor.left - gap - w else anchor.right + gap
        return Placement(clampX(x), clampY(anchor.center.y - h / 2f), if (toStart) Side.Start else Side.End)
    }
    val above = anchor.top - gap - h >= gutter
    val y = if (above) anchor.top - gap - h else anchor.bottom + gap
    return Placement(clampX(anchor.center.x - w / 2f), clampY(y), if (above) Side.Above else Side.Below)
}

@Composable
private fun Bubble(text: AnnotatedString, onClose: () -> Unit, onNoMore: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.testTag(TipTestTags.BUBBLE),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            Row(Modifier.align(Alignment.End)) {
                TextButton(onClick = onNoMore, modifier = Modifier.testTag(TipTestTags.NO_MORE)) {
                    Text(
                        stringResource(R.string.tip_no_more),
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
                    )
                }
                TextButton(onClick = onClose, modifier = Modifier.testTag(TipTestTags.GOT_IT)) {
                    Text(
                        stringResource(R.string.tip_got_it),
                        color = MaterialTheme.colorScheme.inversePrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private class TipAnchor(val owner: Any, val bounds: Rect, val beside: Boolean)

private class TipAnchors {
    private val placed = mutableStateMapOf<Tip, TipAnchor>()

    fun of(tip: Tip): TipAnchor? = placed[tip]

    /** The latest to be laid out wins: of two marks for one tip, only one is ever on screen. */
    fun place(owner: Any, tip: Tip, bounds: Rect, beside: Boolean) {
        val now = placed[tip]
        when {
            bounds.isEmpty -> remove(owner, tip)
            now == null || now.owner !== owner || now.bounds != bounds || now.beside != beside ->
                placed[tip] = TipAnchor(owner, bounds, beside)
        }
    }

    fun remove(owner: Any, tip: Tip) {
        if (placed[tip]?.owner === owner) placed.remove(tip)
    }
}

private val LocalTipAnchors = staticCompositionLocalOf<TipAnchors?> { null }

private data class TipAnchorElement(val tip: Tip, val beside: Boolean) : ModifierNodeElement<TipAnchorNode>() {
    override fun create() = TipAnchorNode(tip, beside)

    override fun update(node: TipAnchorNode) {
        if (node.tip != tip) node.forget()
        node.tip = tip
        node.beside = beside
    }
}

private class TipAnchorNode(var tip: Tip, var beside: Boolean) :
    Modifier.Node(),
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {

    private var anchors: TipAnchors? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val found = anchors ?: currentValueOf(LocalTipAnchors)?.also { anchors = it } ?: return
        found.place(this, tip, coordinates.boundsInRoot(), beside)
    }

    fun forget() {
        anchors?.remove(this, tip)
    }

    override fun onDetach() {
        forget()
        anchors = null
    }
}

private const val RING = "ring"
private const val ARROW = "arrow"
private const val BUBBLE = "bubble"
private val BUBBLE_MAX = 300.dp
