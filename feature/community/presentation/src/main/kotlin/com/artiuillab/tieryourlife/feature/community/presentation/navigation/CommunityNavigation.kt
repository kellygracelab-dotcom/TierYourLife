package com.artiuillab.tieryourlife.feature.community.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.artiuillab.tieryourlife.feature.community.presentation.author.AuthorScreen
import com.artiuillab.tieryourlife.feature.community.presentation.following.FollowingScreen
import com.artiuillab.tieryourlife.feature.community.presentation.list.CommunityListScreen
import com.artiuillab.tieryourlife.feature.community.presentation.moderation.ModerationScreen
import com.artiuillab.tieryourlife.feature.community.presentation.published.MyPublishedScreen
import com.artiuillab.tieryourlife.feature.tier.board.play.BoardPlay
import com.artiuillab.tieryourlife.feature.tier.board.play.NoPlay

fun NavGraphBuilder.communityListScreen(
    onBack: () -> Unit,
    onOpenSaved: (Long) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
    /** Another published list named on this one: the one it was based on. */
    onOpenList: (String) -> Unit = {},
    /** Play over the list; its result fills Mine. */
    play: BoardPlay = NoPlay,
) {
    composable<CommunityRoute.CommunityList> {
        CommunityListScreen(onBack = onBack, onOpenSaved = onOpenSaved, onAuthorClick = onAuthorClick, onOpenList = onOpenList, play = play)
    }
}

/** With what the card showed, so the screen has something to draw before the answer. */
fun NavController.navigateToCommunityList(route: CommunityRoute.CommunityList) {
    navigate(route)
}

/** By id alone, from a place that holds no card: the screen waits with a spinner. */
fun NavController.navigateToCommunityList(publishedId: String) {
    navigate(CommunityRoute.CommunityList(publishedId))
}

fun NavGraphBuilder.authorScreen(
    onBack: () -> Unit,
    onOpenList: (CommunityRoute.CommunityList) -> Unit,
) {
    composable<CommunityRoute.Author> {
        AuthorScreen(onBack = onBack, onOpenList = onOpenList)
    }
}

fun NavController.navigateToAuthor(uid: String, name: String, photoUrl: String?) {
    navigate(CommunityRoute.Author(uid, name, photoUrl))
}

fun NavGraphBuilder.myPublishedScreen(onBack: () -> Unit, onOpen: (CommunityRoute.CommunityList) -> Unit) {
    composable<CommunityRoute.MyPublished> {
        MyPublishedScreen(onBack = onBack, onOpen = onOpen)
    }
}

fun NavController.navigateToMyPublished() {
    navigate(CommunityRoute.MyPublished)
}

fun NavGraphBuilder.followingScreen(
    onBack: () -> Unit,
    onOpenAuthor: (uid: String, name: String, photoUrl: String?) -> Unit,
    onOpenTopic: (CommunityRoute.Topic) -> Unit,
    onOpenSection: (CommunityRoute.Section) -> Unit = {},
) {
    composable<CommunityRoute.Following> {
        FollowingScreen(
            onBack = onBack,
            onOpenAuthor = onOpenAuthor,
            onOpenTopic = { topic ->
                val section = topic.section
                if (section != null) onOpenSection(CommunityRoute.Section(section, topic.id)) else onOpenTopic(CommunityRoute.Topic.of(topic))
            },
            onOpenSection = { onOpenSection(CommunityRoute.Section(it)) },
        )
    }
}

fun NavController.navigateToFollowing() {
    navigate(CommunityRoute.Following)
}

fun NavGraphBuilder.moderationScreen(onBack: () -> Unit, onOpenList: (String) -> Unit) {
    composable<CommunityRoute.Moderation> {
        ModerationScreen(onBack = onBack, onOpenList = onOpenList)
    }
}

fun NavController.navigateToModeration() {
    navigate(CommunityRoute.Moderation)
}
