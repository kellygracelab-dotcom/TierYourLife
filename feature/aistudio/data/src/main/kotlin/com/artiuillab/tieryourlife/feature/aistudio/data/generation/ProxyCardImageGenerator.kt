package com.artiuillab.tieryourlife.feature.aistudio.data.generation

import com.artiuillab.tieryourlife.feature.aistudio.data.remote.api.CardImageApi
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.GenerateImageRequestDto
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.CardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyCardImageGenerator @Inject constructor(
    private val api: CardImageApi,
    private val imageStore: ImageBytesStore,
) : CardImageGenerator {

    override suspend fun generate(prompt: String): GeneratedCardImage {
        val bytes = api.generate(GenerateImageRequestDto(prompt)).use { it.bytes() }
        if (bytes.isEmpty()) throw IOException("Proxy returned an empty image")

        val path = withContext(Dispatchers.IO) { imageStore.save(bytes) }
        return GeneratedCardImage(prompt = prompt, imageUri = "file://$path")
    }

    override suspend fun discard(image: GeneratedCardImage): Unit = withContext(Dispatchers.IO) {
        imageStore.delete(image.imageUri)
    }

    override suspend fun discardAll(): Unit = withContext(Dispatchers.IO) {
        imageStore.clear()
    }
}
