package com.artiuillab.tieryourlife.feature.account.domain.model

/**
 * The square of a picture that becomes the face, as fractions of the
 * picture's own width and height: the same numbers whatever size it is
 * decoded at. A square in pixels, so [width] and [height] differ on a
 * picture that is not square.
 */
data class FaceCrop(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)
