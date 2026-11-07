package com.artiuillab.tieryourlife.core.theme.ui

import android.content.res.Resources
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.artiuillab.tieryourlife.core.theme.R
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import kotlinx.coroutines.flow.Flow

fun UserMessage.PicturesDropped.text(resources: Resources): String =
    resources.getQuantityString(R.plurals.snack_pictures_dropped, count, count)

/** "That did not work" at the bottom, or that a list waits for a moderator, for a screen whose view model says no more. Last child of a Box. */
@Composable
fun ActionFailedSnackbar(messages: Flow<UserMessage>, modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    val failed = stringResource(R.string.snack_action_failed)
    val underReview = stringResource(R.string.snack_published_under_review)
    val staff = stringResource(R.string.snack_target_is_staff)
    val resources = LocalResources.current
    LaunchedEffect(messages) {
        messages.collect {
            hostState.showSnackbar(
                when (it) {
                    UserMessage.PublishedUnderReview -> underReview
                    UserMessage.TargetIsStaff -> staff
                    is UserMessage.PicturesDropped -> it.text(resources)
                    else -> failed
                },
            )
        }
    }
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.fillMaxSize().wrapContentHeight(Alignment.Bottom).navigationBarsPadding(),
    )
}
