package com.artiuillab.tieryourlife.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Role table transcribed from docs/design-spec-turns-8-9.md, section 2 — every unique
// font declaration that actually occurs in the design mocks, with a role name chosen
// from the element it sits on (the mock itself never named roles). Only the M3 slots
// whose app value differs from the M3 default are declared below (titleLarge,
// titleMedium, bodyLarge); headlineSmall, bodyMedium, bodySmall, labelLarge and
// labelMedium are also design-table roles, but their table values are pixel-identical
// to Material3's own defaults for those slots, so composables read
// MaterialTheme.typography.<slot> directly without a redeclaration here. Roles the
// design uses that have no matching M3 slot at all (the tier band letter/caption, the
// swatch letter, tab labels, chip text) live in TierYourLifeExtraType, see Theme.kt.
val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)
