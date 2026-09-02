package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Google's mark, from Google.
 *
 * Their four-colour G is a trademark and their guidelines ask for their own
 * asset rather than a redrawing of it -- a close copy is still a modified
 * trademark, which is the thing being forbidden. This is the file they ship in
 * `play-services-base`, which arrives with the credential library the sign-in
 * already uses, so nothing is copied, resized or recoloured on the way here.
 *
 * Eighteen density-independent pixels because that is the size the asset is
 * drawn for, and the size their guidelines pair with this button height.
 */
@Composable
internal fun GoogleMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(com.google.android.gms.base.R.drawable.googleg_standard_color_18),
        contentDescription = null,
        modifier = modifier.size(18.dp),
    )
}
