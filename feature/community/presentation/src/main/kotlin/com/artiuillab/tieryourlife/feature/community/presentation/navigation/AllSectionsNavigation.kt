package com.artiuillab.tieryourlife.feature.community.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.artiuillab.tieryourlife.feature.community.presentation.allsections.AllSectionsScreen
import com.artiuillab.tieryourlife.feature.community.presentation.propose.ProposeSectionScreen

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

/** The proposal form: a guest is asked to sign in first, and a sent proposal closes it. */
fun NavGraphBuilder.proposeSectionScreen(
    onClose: () -> Unit,
    onSignIn: () -> Unit,
    onOpenSection: (CommunityRoute.Section) -> Unit,
) {
    composable<CommunityRoute.ProposeSection> {
        ProposeSectionScreen(onClose = onClose, onSignIn = onSignIn, onOpenSection = { onOpenSection(CommunityRoute.Section(it)) })
    }
}

fun NavController.navigateToProposeSection(fileUnder: String? = null) {
    navigate(CommunityRoute.ProposeSection(fileUnder))
}
