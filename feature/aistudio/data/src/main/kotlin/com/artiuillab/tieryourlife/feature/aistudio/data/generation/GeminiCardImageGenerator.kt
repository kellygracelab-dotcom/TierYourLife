package com.artiuillab.tieryourlife.feature.aistudio.data.generation

import com.artiuillab.tieryourlife.feature.aistudio.data.remote.api.GeminiImageApi
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.ImageResponseFormatDto
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.InteractionInputDto
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.InteractionRequestDto
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.CardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val GEMINI_MODEL = "gemini-3.1-flash-image"
private const val ASPECT_RATIO = "3:4"
private const val IMAGE_SIZE = "1K"
private const val IMAGE_CONTENT_TYPE = "image"

@Singleton
class GeminiCardImageGenerator @Inject constructor(
    private val api: GeminiImageApi,
    private val imageStore: ImageBytesStore,
    private val base64Decoder: Base64Decoder,
) : CardImageGenerator {

    override suspend fun generate(prompt: String): GeneratedCardImage {
        val request = InteractionRequestDto(
            model = GEMINI_MODEL,
            input = listOf(InteractionInputDto(text = prompt)),
            responseFormat = ImageResponseFormatDto(
                aspectRatio = ASPECT_RATIO,
                imageSize = IMAGE_SIZE,
            ),
        )
        val response = api.createInteraction(request)
        val imageData = response.steps
            .asSequence()
            .flatMap { it.content.asSequence() }
            .firstOrNull { it.type == IMAGE_CONTENT_TYPE && it.data != null }
            ?.data
            ?: throw IOException("Gemini response contained no image")

        val path = withContext(Dispatchers.IO) {
            imageStore.save(base64Decoder.decode(imageData))
        }
        return GeneratedCardImage(prompt = prompt, imageUri = "file://$path")
    }

    override suspend fun discard(image: GeneratedCardImage): Unit = withContext(Dispatchers.IO) {
        imageStore.delete(image.imageUri)
    }

    override suspend fun discardAll(): Unit = withContext(Dispatchers.IO) {
        imageStore.clear()
    }
}
