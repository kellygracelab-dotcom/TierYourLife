package com.artiuillab.tieryourlife.feature.aistudio.domain.generation

import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage

interface CardImageGenerator {
    suspend fun generate(prompt: String): GenerationOutcome
    suspend fun discard(image: GeneratedCardImage)
    suspend fun discardAll()
}
