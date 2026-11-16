package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.InMemoryBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory

/**
 * The theme with a layout memory of the test's own. The local's default is one
 * object for the whole process, so a switch flipped in one test (One at a time,
 * a folded column) stayed on for every test after it.
 */
@Composable
internal fun BoardTestTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBoardLayoutMemory provides remember { InMemoryBoardLayoutMemory() }) {
        TierYourLifeTheme(content = content)
    }
}
