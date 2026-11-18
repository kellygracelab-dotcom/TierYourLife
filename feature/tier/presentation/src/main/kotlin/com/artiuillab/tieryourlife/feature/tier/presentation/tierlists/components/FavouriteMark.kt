package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.ui.StarIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

/**
 * The star a favourite wears, as on the site (artboard 42). Only a mark: a list
 * is starred or unstarred from the selection bar after a long press, so a
 * star on every card is not needed to reach it.
 */
@Composable
internal fun FavouriteMark(id: Long, size: Dp = 20.dp, color: Color? = null) {
    Box(
        Modifier.size(size).clearAndSetSemantics { testTag = TierListsTestTags.star(id) },
        contentAlignment = Alignment.Center,
    ) {
        StarIcon(on = true, size = size, colorOverride = color)
    }
}

@Composable
internal fun favouriteLabel(favourite: Boolean): String =
    stringResource(if (favourite) R.string.lists_favourite_remove else R.string.lists_favourite_add)

/** A reader who cannot long-press keeps starring as an action of the card itself. */
internal fun Modifier.favouriteAction(label: String, onToggle: (() -> Unit)?): Modifier {
    if (onToggle == null) return this
    return semantics {
        customActions = listOf(
            CustomAccessibilityAction(label) {
                onToggle()
                true
            },
        )
    }
}
