package com.artiuillab.tieryourlife.feature.community.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.artiuillab.tieryourlife.feature.community.presentation.topic.TopicScreen

fun NavGraphBuilder.topicScreen(
    onBack: () -> Unit,
    onOpenList: (CommunityRoute.CommunityList) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
    onSignIn: () -> Unit = {},
    onOpenTopic: (CommunityRoute.Topic) -> Unit = {},
    onOpenSection: (CommunityRoute.Section) -> Unit = {},
) {
    composable<CommunityRoute.Topic> {
        TopicScreen(
            onBack = onBack,
            onOpenList = onOpenList,
            onAuthorClick = onAuthorClick,
            onSignIn = onSignIn,
            onOpenTopic = onOpenTopic,
            onOpenSection = onOpenSection,
        )
    }
}

/** With what the chip showed, so the header is right before the answer. */
fun NavController.navigateToTopic(route: CommunityRoute.Topic) {
    navigate(route)
}
