package com.artiuillab.tieryourlife.core.theme.preview

import androidx.compose.ui.tooling.preview.Preview

/**
 * The windows a screen has to survive, ordered by how much room they give.
 * The cover comes first: every other window is at least twice as tall.
 */
@Preview(
    // A Z Flip 7 cover: 748x720 physical at 340dpi. A screen that only works
    // because there is always more height is caught here.
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
    // Where the rail arrives and the second pane does not: a small tablet, or
    // half a large one. The awkward middle, and the one most often skipped.
    name = "Small tablet",
    device = "spec:width=600dp,height=960dp,dpi=240",
    showSystemUi = false,
)
@Preview(
    name = "Tablet",
    device = "spec:width=800dp,height=1280dp,dpi=240",
    showSystemUi = false,
)
@Preview(
    // A tablet held as a tablet: the rail, the index and the board all at
    // once, which is the only window where all three are on screen together.
    name = "Tablet landscape",
    device = "spec:width=1280dp,height=800dp,dpi=240",
    showSystemUi = false,
)
@Preview(
    name = "Large font",
    device = "spec:width=360dp,height=800dp,dpi=420",
    fontScale = 1.5f,
    showSystemUi = false,
)
annotation class TierYourLifeDevicePreviews
