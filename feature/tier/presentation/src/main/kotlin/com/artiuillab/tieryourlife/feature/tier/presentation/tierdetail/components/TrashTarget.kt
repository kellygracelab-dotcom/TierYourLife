package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags
import kotlin.math.roundToInt

// Mock-literal colors with no matching theme role (same error/errorContainer mismatch
// already hardcoded for the move sheet's "Remove from list" row).
private val RestingBgLight = Color(0xFFF9DEDC)
private val RestingBgDark = Color(0xFF601410)
private val RestingIconLight = Color(0xFF8C1D18)
private val RestingIconDark = Color(0xFFF9DEDC)
private val HoverBgLight = Color(0xFFB3261E)
private val HoverBgDark = Color(0xFFFFB4AB)
private val HoverContentLight = Color(0xFFFFFFFF)
private val HoverContentDark = Color(0xFF601410)

// The drop target for removing a poster. Only ever composed while a tile is lifted —
// it registers itself with the same TierDragController rows and the pool use, so
// hover/drop detection is one mechanism, not a special case next to it.
@Composable
internal fun TrashTarget(
    dragController: TierDragController,
    poolTierId: Long?,
    modifier: Modifier = Modifier,
) {
    val isHovering = dragController.isHoveringTrash
    val isDark = TierYourLifeMedia.current.isDark
    val haptic = LocalHapticFeedback.current
    val label = stringResource(R.string.tier_detail_trash_target_label)
    // The pool applies its own navigationBarsPadding, so anchoring to its actual
    // registered top (rather than a fixed dp guess) keeps the 16dp gap correct on
    // every device instead of drifting into an overlap under a taller inset.
    val poolTop = poolTierId?.let { dragController.tierBounds(it)?.top }

    LaunchedEffect(isHovering) {
        if (isHovering) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Only ever composed mid-drag, so this always fires between one drag and the next —
    // registering fresh every time rather than leaving behind a stale rect that a later
    // drag's very first hover check could read before this drag's own registration runs.
    DisposableEffect(Unit) {
        onDispose { dragController.unregisterTrashBounds() }
    }

    val size = if (isHovering) 72.dp else 56.dp
    val shape = RoundedCornerShape(if (isHovering) 24.dp else 16.dp)
    val elevation = if (isHovering) 8.dp else 3.dp
    val backgroundColor = if (isHovering) {
        if (isDark) HoverBgDark else HoverBgLight
    } else {
        if (isDark) RestingBgDark else RestingBgLight
    }
    val contentColor = if (isHovering) {
        if (isDark) HoverContentDark else HoverContentLight
    } else {
        if (isDark) RestingIconDark else RestingIconLight
    }

    Column(
        modifier = modifier
            .padding(end = 16.dp)
            .size(size)
            .offset {
                // The box's bottom edge sits a fixed 16dp above the pool's top regardless
                // of size, so growing on hover extends upward, not into the pool.
                val top = poolTop?.let { it - 16.dp.toPx() - size.toPx() } ?: -size.toPx()
                IntOffset(x = 0, y = top.roundToInt())
            }
            .shadow(elevation = elevation, shape = shape)
            .clip(shape)
            .background(backgroundColor)
            .onGloballyPositioned { coordinates -> dragController.registerTrashBounds(coordinates.boundsInRoot()) }
            .testTag(TierDetailTestTags.TRASH_TARGET)
            .semantics(mergeDescendants = true) { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isHovering) {
            DeleteFilledIcon(30.dp, contentColor)
            Text(text = label, style = TierYourLifeType.current.trashRemoveLabel, color = contentColor)
        } else {
            DeleteOutlineIcon(24.dp, contentColor)
        }
    }
}
