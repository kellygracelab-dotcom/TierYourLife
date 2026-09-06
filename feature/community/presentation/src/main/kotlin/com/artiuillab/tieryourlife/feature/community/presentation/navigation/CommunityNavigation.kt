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
    onSaved: (Long) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
) {
    composable<CommunityRoute.CommunityList> {
        CommunityListScreen(onBack = onBack, onSaved = onSaved, onAuthorClick = onAuthorClick)
    }
}

fun NavController.navigateToCommunityList(publishedId: String) {
    navigate(CommunityRoute.CommunityList(publishedId))
}

fun NavGraphBuilder.authorScreen(
    onBack: () -> Unit,
    onOpenList: (String) -> Unit,
) {
    composable<CommunityRoute.Author> {
        AuthorScreen(onBack = onBack, onOpenList = onOpenList)
    }
}

fun NavController.navigateToAuthor(uid: String, name: String, photoUrl: String?) {
    navigate(CommunityRoute.Author(uid, name, photoUrl))
}

fun NavGraphBuilder.myPublishedScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
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
