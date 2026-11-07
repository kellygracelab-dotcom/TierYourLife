package com.artiuillab.tieryourlife.feature.community.presentation.follow

import android.content.res.Resources
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import com.artiuillab.tieryourlife.feature.community.domain.model.MAX_FOLLOWING
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionWasMerged
import com.artiuillab.tieryourlife.feature.community.domain.model.TooManyFollowed
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CommunityNotice
import kotlinx.coroutines.flow.Flow

/** Why a follow or an unfollow did not happen, as a screen says it. */
enum class FollowRefusal {
    Follow,
    Unfollow,
    TooMany,

    /** A follow of a section merged away into nothing a follow can follow. */
    Merged,
    ;

    companion object {
        fun of(following: Boolean, error: Throwable): FollowRefusal = when {
            error is TooManyFollowed -> TooMany
            error is SectionWasMerged && following -> Merged
            following -> Follow
            else -> Unfollow
        }
    }
}

fun Resources.sentenceFor(refusal: FollowRefusal): String = when (refusal) {
    FollowRefusal.Follow -> getString(R.string.explore_follow_failed)
    FollowRefusal.Unfollow -> getString(R.string.follow_unfollow_failed)
    FollowRefusal.TooMany -> getString(R.string.follow_too_many, MAX_FOLLOWING)
    FollowRefusal.Merged -> getString(R.string.follow_section_merged)
}

internal fun Resources.followingSentence(notice: CommunityNotice.NowFollowing, name: String): String =
    getString(if (notice.moved) R.string.follow_section_moved else R.string.explore_now_following, name)

/** The refusals of a screen that has no snackbar of its own for them. */
@Composable
internal fun FollowRefusalSnackbar(refusals: Flow<FollowRefusal>, modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(refusals) {
        refusals.collect { hostState.showSnackbar(resources.sentenceFor(it)) }
    }
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.fillMaxSize().wrapContentHeight(Alignment.Bottom).navigationBarsPadding(),
    )
}

/** Opens the sign-in each time a guest's Follow asks for it. */
@Composable
internal fun SignInToFollow(requests: Flow<Unit>, onSignIn: () -> Unit) {
    val signIn by rememberUpdatedState(onSignIn)
    LaunchedEffect(requests) { requests.collect { signIn() } }
}
