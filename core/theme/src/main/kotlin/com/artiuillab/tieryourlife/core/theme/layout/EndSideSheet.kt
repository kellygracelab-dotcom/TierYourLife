package com.artiuillab.tieryourlife.core.theme.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** A side sheet's width: wide enough for a search and its results, narrow enough to leave the board in view. */
val SideSheetWidth: Dp = 360.dp

/**
 * A sheet that comes in from the end edge and leaves the rest of the window
 * in view, for a board laid out in columns: what is added is seen landing in
 * the pool beside it. Elsewhere [AdaptiveSheet] is the one to use.
 */
@Composable
fun EndSideSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = SideSheetWidth,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.CenterEnd) {
            Surface(
                modifier = modifier
                    .width(width)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Column(content = content)
            }
        }
    }
}
