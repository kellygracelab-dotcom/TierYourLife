package com.artiuillab.tieryourlife.core.theme.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val ContainerLight = Color(0xFF303036)
private val ContainerDark = Color(0xFFE4E1E9)
private val ContentLight = Color(0xFFF3F0F7)
private val ContentDark = Color(0xFF1B1B21)
private val ActionLight = Color(0xFFBAC3FF)
private val ActionDark = Color(0xFF4A5BAA)

/** A snackbar that goes either way with a swipe. */
@Composable
fun DeletedItemSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val isDark = TierYourLifeMedia.current.isDark

    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        key(data) {
            val swipe = rememberSwipeToDismissBoxState()
            LaunchedEffect(swipe.currentValue) {
                if (swipe.currentValue != SwipeToDismissBoxValue.Settled) data.dismiss()
            }
            SwipeToDismissBox(state = swipe, backgroundContent = {}) {
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.testTag(HomeBarTestTags.DELETED_ITEM_SNACKBAR),
                    shape = RoundedCornerShape(4.dp),
                    containerColor = if (isDark) ContainerDark else ContainerLight,
                    contentColor = if (isDark) ContentDark else ContentLight,
                    actionColor = if (isDark) ActionDark else ActionLight,
                )
            }
        }
    }
}

/**
 * One snackbar at a time: a new one takes the place of the one showing, which
 * ends as dismissed, rather than waiting behind it.
 */
class ReplacingSnackbar(private val host: SnackbarHostState, private val scope: CoroutineScope) {
    private var showing: Job? = null
    private var replaced: (() -> Unit)? = null

    fun show(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onResult: (SnackbarResult) -> Unit = {},
    ) {
        showing?.cancel()
        replaced?.invoke()
        val ended = { onResult(SnackbarResult.Dismissed) }
        replaced = ended
        showing = scope.launch {
            val result = host.showSnackbar(message = message, actionLabel = actionLabel, duration = duration)
            if (replaced === ended) replaced = null
            onResult(result)
        }
    }
}
