package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

/**
 * The star that keeps a board at the top.
 *
 * Announced as a checkbox rather than a button, because that is what it is:
 * it has a state, and a reader who cannot see the fill needs to be told which
 * one it is in.
 */
@Composable
internal fun StarButton(on: Boolean, id: Long, onClick: () -> Unit, size: Dp = 48.dp) {
    val description = stringResource(
        if (on) R.string.lists_favourite_remove else R.string.lists_favourite_add,
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .semantics {
                contentDescription = description
                role = Role.Checkbox
                toggleableState = if (on) ToggleableState.On else ToggleableState.Off
            }
            .testTag(TierListsTestTags.star(id)),
    ) {
        StarIcon(on = on)
    }
}
