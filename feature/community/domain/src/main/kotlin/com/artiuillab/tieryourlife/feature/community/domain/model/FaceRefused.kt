package com.artiuillab.tieryourlife.feature.community.domain.model

/**
 * The server looked at the picture and will not show it as a face: another picture, not another try. [unchecked]
 * when it could not judge it, an animation perhaps: a still photo may go.
 */
class FaceRefused(val unchecked: Boolean = false) : Exception("The picture may not be a face")
