package com.artiuillab.tieryourlife.feature.aistudio.data.credits

import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import javax.inject.Inject
import javax.inject.Singleton

/** Nothing is counted when the images are drawn on the device. */
@Singleton
class UnmeteredGenerationCredits @Inject constructor() : GenerationCredits {

    override suspend fun remaining(): Int? = null
}
