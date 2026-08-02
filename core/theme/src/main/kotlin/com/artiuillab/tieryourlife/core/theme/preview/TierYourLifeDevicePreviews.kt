package com.artiuillab.tieryourlife.core.theme.preview

import androidx.compose.ui.tooling.preview.Preview

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
