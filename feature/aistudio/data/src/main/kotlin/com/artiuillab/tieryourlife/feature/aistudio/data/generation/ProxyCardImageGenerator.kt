package com.artiuillab.tieryourlife.feature.aistudio.data.generation

import com.artiuillab.tieryourlife.feature.aistudio.data.remote.api.CardImageApi
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.GenerateImageRequestDto
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.CardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.GenerationOutcome
import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
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
        } catch (e: IOException) {
            Timber.w(e, "Could not reach the server to generate a card")
            return GenerationOutcome.Failed
        }

        if (!response.isSuccessful) {
            response.errorBody()?.close()
            return if (response.code() == HTTP_PAYMENT_REQUIRED) {
                GenerationOutcome.OutOfCredits
            } else {
                Timber.w("Generating a card was refused with %d", response.code())
                GenerationOutcome.Failed
            }
        }

        // A credit is spent by the time we get here, so an answer that arrives
        // empty is worth naming rather than folding into the same Failed.
        val bytes = response.body()?.use { it.bytes() }
        if (bytes == null || bytes.isEmpty()) {
            Timber.w("The server accepted the generation and sent back no image")
            return GenerationOutcome.Failed
        }

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
