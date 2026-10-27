package com.artiuillab.tieryourlife.feature.community.presentation.moderation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** The chips over a queue as its first item: they leave with the queue instead of holding a row of their own. */
internal fun LazyListScope.chipsItem(chips: (@Composable () -> Unit)?) {
    if (chips != null) item(key = "chips", contentType = "chips") { chips() }
}

/**
 * What stands in for a queue with nothing to list yet (loading, failed,
 * empty): most of the height under the chips, its content centred in it.
 */
internal fun LazyListScope.stateItem(content: @Composable BoxScope.() -> Unit) {
    item(key = "state", contentType = "state") {
        Box(
            modifier = Modifier.fillParentMaxWidth().fillParentMaxHeight(STATE_SHARE),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

private const val STATE_SHARE = 0.8f
