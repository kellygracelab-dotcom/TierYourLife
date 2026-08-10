package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags

// Mock-literal colors: the app's inverseSurface/inverseOnSurface are the stock M3
// baseline tones (close enough to pass for them), but inversePrimary is stock purple,
// not this app's own primary — the mock's action colour is this app's primary from
// the *other* theme, which isn't a role this app's ColorScheme actually wires up.
private val ContainerLight = Color(0xFF303036)
private val ContainerDark = Color(0xFFE4E1E9)
private val ContentLight = Color(0xFFF3F0F7)
private val ContentDark = Color(0xFF1B1B21)
private val ActionLight = Color(0xFFBAC3FF)
private val ActionDark = Color(0xFF4A5BAA)

@Composable
internal fun DeletedItemSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val isDark = TierYourLifeMedia.current.isDark

    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            modifier = Modifier.testTag(TierDetailTestTags.DELETED_ITEM_SNACKBAR),
            shape = RoundedCornerShape(4.dp),
            containerColor = if (isDark) ContainerDark else ContainerLight,
            contentColor = if (isDark) ContentDark else ContentLight,
            actionColor = if (isDark) ActionDark else ActionLight,
        )
    }
}
