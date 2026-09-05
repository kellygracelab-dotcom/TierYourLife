package com.artiuillab.tieryourlife.core.settings

/**
 * Things the app has built and is not offering yet. A switch rather than a
 * deletion: the code and its tests keep compiling and running, so nothing
 * rots while it waits.
 */
object Features {

    /**
     * Card pictures with Gemini. Off for the first release: every generation
     * costs money and nothing charges for one yet. The server is the real
     * switch (`OFFER_GENERATION`); this only decides whether somebody is
     * offered something that would fail.
     */
    const val GENERATION_OFFERED = false
}
