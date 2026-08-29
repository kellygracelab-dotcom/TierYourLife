package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeleteFilledIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeleteOutlineIcon
import kotlin.math.roundToInt

private val RestingBgLight = Color(0xFFF9DEDC)
private val RestingBgDark = Color(0xFF601410)
private val RestingIconLight = Color(0xFF8C1D18)
private val RestingIconDark = Color(0xFFF9DEDC)
private val HoverBgLight = Color(0xFFB3261E)
private val HoverBgDark = Color(0xFFFFB4AB)
private val HoverContentLight = Color(0xFFFFFFFF)
private val HoverContentDark = Color(0xFF601410)

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
    val poolTop = poolTierId?.let { dragController.tierBounds(it)?.top }

    LaunchedEffect(isHovering) {
        if (isHovering) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

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
        // No caption under the icon: it repeated what the icon says, at a size
        // that would clip in half our languages, for the two seconds a finger
        // hovers. The content description above carries it where it matters.
        if (isHovering) {
            DeleteFilledIcon(30.dp, contentColor)
        } else {
            DeleteOutlineIcon(24.dp, contentColor)
        }
    }
}
