package com.artiuillab.tieryourlife.core.theme.type

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// App-specific type roles with no Material 3 equivalent.
data class TierYourLifeExtraType(
    val tierBandLetter: TextStyle,
    val tierBandCaption: TextStyle,
    val tierSwatchLetter: TextStyle,
    val tabLabel: TextStyle,
    val captionUnderTitle: TextStyle,
    val chipText: TextStyle,
    val trashRemoveLabel: TextStyle,
)

private val ExtraType = TierYourLifeExtraType(
    tierBandLetter = TextStyle(fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 28.sp),
    tierBandCaption = TextStyle(fontWeight = FontWeight.Normal, fontSize = 10.sp, lineHeight = 12.sp),
    tierSwatchLetter = TextStyle(fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 24.sp),
    tabLabel = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    captionUnderTitle = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    chipText = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp),
    trashRemoveLabel = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 12.sp),
)

object TierYourLifeType {
    val current: TierYourLifeExtraType = ExtraType
}
