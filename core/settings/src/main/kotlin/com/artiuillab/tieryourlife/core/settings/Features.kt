package com.artiuillab.tieryourlife.core.settings

/**
 * Things the app has built and is not offering yet.
 *
 * A switch rather than a deletion, and one switch rather than several: the
 * code, its tests and its screens stay exactly where they are and keep being
 * compiled and run, so nothing rots while it waits. Turning something back on
 * is a one-line change and a release.
 */
object Features {

    /**
     * Making card pictures with Gemini.
     *
     * Off for the first release. Every generation costs money and there is
     * nothing yet that charges for one, and an app that generates images has
     * to answer more of Google's questions on the way through review than an
     * app that does not -- neither is worth it before anybody is using this.
     *
     * The server is the real switch: `OFFER_GENERATION` there refuses the
     * request whatever the app shows, because hiding a button does not stop
     * one being sent. This only decides whether somebody is offered something
     * that would fail.
     */
    const val GENERATION_OFFERED = false
}
