package com.artiuillab.tieryourlife.feature.aistudio.data.generation

import com.artiuillab.tieryourlife.feature.aistudio.data.remote.api.CardImageApi
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.GenerateImageRequestDto
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.CardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.GenerationOutcome
import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val HTTP_PAYMENT_REQUIRED = 402
private const val CREDITS_HEADER = "X-Credits-Remaining"

@Singleton
class ProxyCardImageGenerator @Inject constructor(
    private val api: CardImageApi,
    private val imageStore: ImageBytesStore,
) : CardImageGenerator {

    override suspend fun generate(prompt: String): GenerationOutcome {
        val response = try {
            api.generate(GenerateImageRequestDto(prompt))
        } catch (_: IOException) {
            return GenerationOutcome.Failed
        }

        if (!response.isSuccessful) {
            response.errorBody()?.close()
            return if (response.code() == HTTP_PAYMENT_REQUIRED) {
                GenerationOutcome.OutOfCredits
            } else {
                GenerationOutcome.Failed
            }
        }

        val bytes = response.body()?.use { it.bytes() } ?: return GenerationOutcome.Failed
        if (bytes.isEmpty()) return GenerationOutcome.Failed

        val path = withContext(Dispatchers.IO) { imageStore.save(bytes) }
        return GenerationOutcome.Success(
            image = GeneratedCardImage(prompt = prompt, imageUri = "file://$path"),
            creditsRemaining = response.headers()[CREDITS_HEADER]?.toIntOrNull(),
        )
    }

    override suspend fun discard(image: GeneratedCardImage): Unit = withContext(Dispatchers.IO) {
        imageStore.delete(image.imageUri)
    }

    override suspend fun discardAll(): Unit = withContext(Dispatchers.IO) {
        imageStore.clear()
    }
}
