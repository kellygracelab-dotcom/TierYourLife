package com.artiuillab.tieryourlife.feature.tier.domain.model

/**
 * Which part of a cover picture to show: the point of the picture, as shares
 * of its width and height, that stands in the middle of every frame, and how
 * far in from the largest crop the frame allows. The same on every platform
 * (_coordination/cover-focus-2026-11-15/SPEC.md).
 */
data class CoverFocus(
    val x: Float = CENTRE,
    val y: Float = CENTRE,
    val zoom: Float = MIN_ZOOM,
) {
    /**
     * Kept inside what the account accepts, so a drag or a pinch can never ask
     * for more, and as finely as the site keeps it: x and y to four places,
     * the zoom to two. Both write it into the board's fingerprint and must
     * agree to the digit.
     */
    fun inRange(): CoverFocus = CoverFocus(
        x = Math.round(x.coerceIn(0f, 1f).toDouble() * PLACE_STEPS) / PLACE_STEPS.toFloat(),
        y = Math.round(y.coerceIn(0f, 1f).toDouble() * PLACE_STEPS) / PLACE_STEPS.toFloat(),
        zoom = Math.round(zoom.coerceIn(MIN_ZOOM, MAX_ZOOM).toDouble() * ZOOM_STEPS) / ZOOM_STEPS.toFloat(),
    )

    /** As the fingerprint spells it on every platform: whole steps, rounded half up. */
    val fingerprintParts: List<String>
        get() = listOf(
            Math.round(x.toDouble() * PLACE_STEPS),
            Math.round(y.toDouble() * PLACE_STEPS),
            Math.round(zoom.toDouble() * ZOOM_STEPS),
        ).map(Long::toString)

    companion object {
        const val CENTRE = 0.5f
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 4f

        /** What a cover without a focus is drawn with, as it always was. */
        val CENTRED = CoverFocus()

        /** A tall picture without a focus: a portrait's face and a poster's title sit above the middle. */
        val TALL = CoverFocus(y = TALL_Y)

        /** What a [width]×[height] picture shows when nobody chose: [TALL] past 1.2 high to 1 wide, else the centre. */
        fun defaultFor(width: Double, height: Double): CoverFocus =
            if (width > 0 && height / width > TALL_RATIO) TALL else CENTRED

        private const val TALL_RATIO = 1.2
        private const val TALL_Y = 0.35f

        private const val PLACE_STEPS = 10_000L
        private const val ZOOM_STEPS = 100L
    }
}

/** The chosen focus, or what a picture of this size shows without one. */
fun CoverFocus?.orDefaultFor(width: Double, height: Double): CoverFocus = this ?: CoverFocus.defaultFor(width, height)

/** A part of a picture, in its own pixels. */
data class CoverFrame(val left: Double, val top: Double, val width: Double, val height: Double)

/**
 * The part of a [width]×[height] picture a frame of [aspect] (width over
 * height) shows for [focus]: the largest crop of that shape, [CoverFocus.zoom]
 * times smaller, centred on the focus and then kept inside the picture.
 */
fun coverFrame(width: Float, height: Float, aspect: Float, focus: CoverFocus?): CoverFrame {
    val (x, y, zoom) = focus.orDefaultFor(width.toDouble(), height.toDouble())
    return coverFrame(width.toDouble(), height.toDouble(), aspect.toDouble(), x.toDouble(), y.toDouble(), zoom.toDouble())
}

/** The same in doubles, as the server and the site work it out, so all three agree to the shared vectors' millionth. */
fun coverFrame(width: Double, height: Double, aspect: Double, x: Double, y: Double, zoom: Double): CoverFrame {
    val wider = width / height > aspect
    val cropWidth = (if (wider) height * aspect else width) / zoom
    val cropHeight = (if (wider) height else width / aspect) / zoom
    return CoverFrame(
        left = (x * width - cropWidth / 2).coerceIn(0.0, width - cropWidth),
        top = (y * height - cropHeight / 2).coerceIn(0.0, height - cropHeight),
        width = cropWidth,
        height = cropHeight,
    )
}
