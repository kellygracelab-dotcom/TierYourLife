package com.artiuillab.tieryourlife.core.theme.preview

import androidx.compose.ui.tooling.preview.Preview

/**
 * The windows a screen has to survive, so that the answer arrives while it is
 * being written rather than on somebody's phone.
 *
 * Ordered by how much room they give. The cover screen comes first because it
 * is the one that takes room away in the direction nothing else does: every
 * other window here is at least twice as tall as it is.
 */
@Preview(
    // A Samsung Flip's cover screen, measured off a Z Flip 7: 748x720 physical
    // at 340dpi. Barely taller than it is wide, which no phone is, and 339dp is
    // less than half of what the shortest phone below gives. A screen that only
    // works because there is always more height is caught here.
    name = "Flip cover",
    device = "spec:width=352dp,height=339dp,dpi=340",
    showSystemUi = false,
)
@Preview(
    name = "Small phone",
    device = "spec:width=320dp,height=720dp,dpi=420",
    showSystemUi = false,
)
@Preview(
    name = "Standard phone",
    device = "spec:width=412dp,height=915dp,dpi=420",
    showSystemUi = false,
)
@Preview(
    name = "Phone landscape",
    device = "spec:width=800dp,height=360dp,dpi=420",
    showSystemUi = false,
)
@Preview(
    name = "Tablet",
    device = "spec:width=800dp,height=1280dp,dpi=240",
    showSystemUi = false,
)
@Preview(
    name = "Large font",
    device = "spec:width=360dp,height=800dp,dpi=420",
    fontScale = 1.5f,
    showSystemUi = false,
)
annotation class TierYourLifeDevicePreviews
