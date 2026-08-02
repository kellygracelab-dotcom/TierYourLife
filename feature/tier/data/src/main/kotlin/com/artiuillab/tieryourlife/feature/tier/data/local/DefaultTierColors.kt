package com.artiuillab.tieryourlife.feature.tier.data.local

// Lives in data rather than theme: these values are written to the database when a tier list is created
// and can later be edited by the user.
// Both variants are stored because the dark color cannot be derived from the light one:
// Material 3 tonal utilities (TonalPalette, HctSolver) are internal.
internal object DefaultTierColors {
    const val S_LIGHT = "#B03A32"
    const val S_DARK = "#F1948C"
    const val A_LIGHT = "#C06A25"
    const val A_DARK = "#E9A867"
    const val B_LIGHT = "#A98B1F"
    const val B_DARK = "#D8C05A"
    const val C_LIGHT = "#3F7F55"
    const val C_DARK = "#7FC393"
    const val D_LIGHT = "#3C6E99"
    const val D_DARK = "#86B8DE"

    const val POOL_LIGHT = "#DAD7E0"
    const val POOL_DARK = "#46464F"
}
