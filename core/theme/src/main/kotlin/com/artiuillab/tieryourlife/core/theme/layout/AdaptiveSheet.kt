package com.artiuillab.tieryourlife.core.theme.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * How wide a task gets when it stops being a sheet.
 *
 * Two sizes, because there are two kinds of task. One is work -- searching a
 * catalogue, building a tier -- with a list to scroll and something to type.
 * The other is a choice from a handful of options, and a radio list 560dp wide
 * puts its label a thumb's width from the control it belongs to.
 */
object SheetWidth {
    val Working: Dp = 560.dp
    val Choosing: Dp = 400.dp
}

/**
 * A task that arrives from the bottom on a phone and in the middle on anything
 * wider.
 *
 * A bottom sheet is a phone shape: it is thumb-reachable, and it covers the
 * bottom of a screen that is mostly out of reach anyway. Neither is true on a
 * tablet, where the same sheet becomes a strip of content along the bottom
 * edge of a mostly empty window, as far from the eye as the layout can manage.
 *
 * Docked surfaces are not sheets and do not come through here -- the pool at
 * the bottom of a board stays exactly where it is at every width.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = SheetWidth.Choosing,
    maxHeight: Dp = 560.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!currentWindowShape.hasRail) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = modifier,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            content = content,
        )
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = modifier
                    .widthIn(max = width)
                    .heightIn(max = maxHeight),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
            ) {
                Column(content = content)
            }
        }
    }
}
