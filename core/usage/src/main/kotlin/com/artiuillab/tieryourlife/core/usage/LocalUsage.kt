package com.artiuillab.tieryourlife.core.usage

import androidx.compose.runtime.staticCompositionLocalOf

/** For what a screen does on its own, with no view model to ask: a share, a copied link, a picture chosen. */
val LocalUsage = staticCompositionLocalOf<Usage> { Usage.None }
