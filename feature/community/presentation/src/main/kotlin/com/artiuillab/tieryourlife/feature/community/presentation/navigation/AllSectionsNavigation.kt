package com.artiuillab.tieryourlife.feature.community.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.artiuillab.tieryourlife.feature.community.presentation.allsections.AllSectionsScreen

/** Every section; "Propose a section" stays hidden while [onProposeSection] is null. */
fun NavGraphBuilder.allSectionsScreen(
    onBack: () -> Unit,
    onOpenSection: (CommunityRoute.Section) -> Unit,
    onProposeSection: (() -> Unit)? = null,
) {
    composable<CommunityRoute.AllSections> {
        AllSectionsScreen(onBack = onBack, onOpenSection = onOpenSection, onProposeSection = onProposeSection)
    }
}

fun NavController.navigateToAllSections() {
    navigate(CommunityRoute.AllSections)
}
