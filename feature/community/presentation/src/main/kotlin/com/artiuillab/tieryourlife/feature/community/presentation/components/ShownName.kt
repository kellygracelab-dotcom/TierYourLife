package com.artiuillab.tieryourlife.feature.community.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.artiuillab.tieryourlife.feature.community.presentation.R

/** What a person is called on screen: one who never gave a name is still someone. */
@Composable
internal fun shownName(name: String): String = name.ifBlank { stringResource(R.string.community_author_unnamed) }

/** A moderator sees no name on an anonymous list; its owner sees theirs, marked. */
@Composable
internal fun reportAuthorName(name: String, anonymous: Boolean): String = when {
    !anonymous -> shownName(name)
    name.isBlank() -> stringResource(R.string.community_anonymous)
    else -> "$name · ${stringResource(R.string.community_anonymous)}"
}
