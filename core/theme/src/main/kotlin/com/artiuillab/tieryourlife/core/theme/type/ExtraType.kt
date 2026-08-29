package com.artiuillab.tieryourlife.core.theme.type

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The four roles Material 3 has no place for. Everything else on screen uses
 * the standard scale; a role here has to earn its keep.
 */
data class TierYourLifeExtraType(
    val tierBandLetter: TextStyle,
    val tierBandCaption: TextStyle,
    val tierSwatchLetter: TextStyle,
    /**
     * The missing step between labelMedium (12/16) and bodyMedium (14/20).
     * Tab labels, the line under a title, and chip text are all this size --
     * they were three roles saying the same thing.
     */
    val supportingLabel: TextStyle,
)

private val ExtraType = TierYourLifeExtraType(
    tierBandLetter = TextStyle(fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 28.sp),
    // 11/16 rather than 10/12: a 1.2 ratio at 10sp leaves no room for what hangs
    // off a letter, and Ukrainian, Arabic and Vietnamese all hang off letters.
    // The clipping would never show in English, which is the dangerous part.
    tierBandCaption = TextStyle(fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp),
    tierSwatchLetter = TextStyle(fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 24.sp),
    supportingLabel = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
)

object TierYourLifeType {
    val current: TierYourLifeExtraType = ExtraType
}
