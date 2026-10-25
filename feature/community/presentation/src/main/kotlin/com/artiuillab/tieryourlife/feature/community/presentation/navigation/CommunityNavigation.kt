package com.artiuillab.tieryourlife.feature.community.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.artiuillab.tieryourlife.feature.community.presentation.author.AuthorScreen
import com.artiuillab.tieryourlife.feature.community.presentation.list.CommunityListScreen
import com.artiuillab.tieryourlife.feature.community.presentation.moderation.ModerationScreen
import com.artiuillab.tieryourlife.feature.community.presentation.published.MyPublishedScreen

fun NavGraphBuilder.communityListScreen(
    onBack: () -> Unit,
    onOpenSaved: (Long) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
) {
    composable<CommunityRoute.CommunityList> {
        CommunityListScreen(onBack = onBack, onOpenSaved = onOpenSaved, onAuthorClick = onAuthorClick)
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

fun NavGraphBuilder.moderationScreen(onBack: () -> Unit, onOpenList: (String) -> Unit) {
    composable<CommunityRoute.Moderation> {
        ModerationScreen(onBack = onBack, onOpenList = onOpenList)
    }
}

fun NavController.navigateToModeration() {
    navigate(CommunityRoute.Moderation)
}
