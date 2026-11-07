package com.artiuillab.tieryourlife.feature.community.domain.model

/** The server looked at the picture and will not show it as a face: another picture, not another try. */
class FaceRefused : Exception("The picture may not be a face")
