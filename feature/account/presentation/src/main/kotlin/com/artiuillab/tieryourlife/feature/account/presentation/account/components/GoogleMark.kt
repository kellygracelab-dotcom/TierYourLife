package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Google's own asset from `play-services-base`, not a redrawing: the G is a
 * trademark and their guidelines forbid copies. 18dp is the size the asset is
 * drawn for and their guidelines pair with this button height.
 */
@Composable
internal fun GoogleMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(com.google.android.gms.base.R.drawable.googleg_standard_color_18),
        contentDescription = null,
        modifier = modifier.size(18.dp),
    )
}
