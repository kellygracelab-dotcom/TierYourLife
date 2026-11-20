package com.artiuillab.tieryourlife.core.theme.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.artiuillab.tieryourlife.core.theme.R

/** The app's own face where it stands as an author: the launcher's artwork, cut as a launcher shows it. */
@Composable
fun AppMark(size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape), contentAlignment = Alignment.Center) {
        val art = Modifier.requiredSize(size * ADAPTIVE_ICON / ADAPTIVE_ICON_SHOWN)
        Image(painterResource(R.drawable.ic_launcher_background), contentDescription = null, modifier = art)
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = art)
    }
}

private const val ADAPTIVE_ICON = 108f
private const val ADAPTIVE_ICON_SHOWN = 72f
