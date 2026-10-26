package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.ui.DeleteIcon
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TierDragController

private val DROP_LABEL = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)

/**
 * Where the trash goes while a card is lifted and the pool stands beside the
 * rows or docked under them: in the pool's head, so the two answers to "not
 * in a tier" sit together. A drop here deletes, with the same Undo as the
 * floating trash; a drop on the pool's cards below unranks.
 */
@Composable
internal fun PoolHeadTrash(dragController: TierDragController, caption: String, modifier: Modifier = Modifier) {
    val hovering = dragController.isHoveringTrash
    val haptic = LocalHapticFeedback.current
    val label = stringResource(R.string.tier_detail_drop_to_delete)
    LaunchedEffect(hovering) {
        if (hovering) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    DisposableEffect(Unit) {
        onDispose { dragController.unregisterTrashBounds() }
    }
    val shape = RoundedCornerShape(12.dp)
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(if (hovering) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer)
                .onGloballyPositioned { coordinates -> dragController.registerTrashBounds(coordinates.boundsInRoot()) }
                .testTag(TierDetailTestTags.TRASH_TARGET)
                .semantics(mergeDescendants = true) { contentDescription = label }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            val content = if (hovering) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer
            DeleteIcon(20.dp, content)
            Spacer(Modifier.width(8.dp))
            Text(text = label, style = DROP_LABEL, color = content, maxLines = 1)
        }
        Text(
            text = caption,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The same, for the folded strip: the icon alone, the strip's width. */
@Composable
internal fun PoolStripTrash(dragController: TierDragController, modifier: Modifier = Modifier) {
    val hovering = dragController.isHoveringTrash
    val label = stringResource(R.string.tier_detail_drop_to_delete)
    DisposableEffect(Unit) {
        onDispose { dragController.unregisterTrashBounds() }
    }
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (hovering) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer)
            .then(if (hovering) Modifier.border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp)) else Modifier)
            .onGloballyPositioned { coordinates -> dragController.registerTrashBounds(coordinates.boundsInRoot()) }
            .testTag(TierDetailTestTags.TRASH_TARGET)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        DeleteIcon(20.dp, if (hovering) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer)
    }
}
