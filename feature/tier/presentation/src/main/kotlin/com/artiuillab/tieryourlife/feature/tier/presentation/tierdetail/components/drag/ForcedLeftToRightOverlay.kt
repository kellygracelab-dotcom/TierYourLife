package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@Composable
internal fun ForcedLeftToRightOverlay(content: @Composable BoxScope.(Offset) -> Unit) {
    // Where this overlay itself begins, in the window's own coordinates.
    //
    // The pointer is reported against the window, and the overlay used to be
    // the window -- so the two agreed and nobody had to say so. Standing a
    // column of boards beside the board moved this four hundred points to the
    // right, and every dragged card jumped the same distance: the offset was
    // being read as "from the left of the screen" and applied as "from the
    // left of the pane".
    var origin by remember { mutableStateOf(Offset.Zero) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates -> origin = coordinates.positionInRoot() },
        ) {
            content(origin)
        }
    }
}
