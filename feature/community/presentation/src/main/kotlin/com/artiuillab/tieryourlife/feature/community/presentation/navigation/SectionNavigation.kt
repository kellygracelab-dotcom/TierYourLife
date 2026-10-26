package com.artiuillab.tieryourlife.feature.community.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionScreen

/** A section's page; a topic in the route opens it with that topic chosen. */
fun NavGraphBuilder.sectionScreen(
    onBack: () -> Unit,
    onOpenList: (CommunityRoute.CommunityList) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
) {
    composable<CommunityRoute.Section> {
        SectionScreen(onBack = onBack, onOpenList = onOpenList, onAuthorClick = onAuthorClick)
    }
}

fun NavController.navigateToSection(route: CommunityRoute.Section) {
    navigate(route)
}
