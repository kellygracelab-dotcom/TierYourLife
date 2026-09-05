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
 * Two widths for two kinds of task: work, with a list to scroll and something
 * to type; and a choice from a few options, where a 560dp radio list puts its
 * label a thumb's width from the control.
 */
object SheetWidth {
    val Working: Dp = 560.dp
    val Choosing: Dp = 400.dp
}

/**
 * From the bottom on a phone, in the middle on anything wider: on a tablet a
 * bottom sheet becomes a strip along the bottom of an empty window.
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
