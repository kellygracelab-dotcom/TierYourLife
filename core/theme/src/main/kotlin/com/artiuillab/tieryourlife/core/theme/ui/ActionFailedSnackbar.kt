package com.artiuillab.tieryourlife.core.theme.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.artiuillab.tieryourlife.core.theme.R
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import kotlinx.coroutines.flow.Flow

/** "That did not work" at the bottom, for a screen whose view model only ever says that. Last child of a Box. */
@Composable
fun ActionFailedSnackbar(messages: Flow<UserMessage>, modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    val failed = stringResource(R.string.snack_action_failed)
    LaunchedEffect(messages) {
        messages.collect { hostState.showSnackbar(failed) }
    }
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.fillMaxSize().wrapContentHeight(Alignment.Bottom),
    )
}
