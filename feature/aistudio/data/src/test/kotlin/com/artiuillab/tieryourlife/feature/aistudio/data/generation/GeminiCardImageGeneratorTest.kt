package com.artiuillab.tieryourlife.feature.aistudio.data.generation

import com.artiuillab.tieryourlife.feature.aistudio.data.remote.api.GeminiImageApi
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.InteractionContentDto
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.InteractionRequestDto
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.InteractionResponseDto
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.InteractionStepDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class GeminiCardImageGeneratorTest {

    @Test
    fun `successful response saves the decoded bytes and returns a file uri`() = runBlocking {
        val response = InteractionResponseDto(
            id = "interaction-1",
            status = "completed",
            steps = listOf(
                InteractionStepDto(
                    type = "model_output",
                    content = listOf(
                        InteractionContentDto(type = "text", text = "Here you go."),
                        InteractionContentDto(type = "image", data = "encoded", mimeType = "image/png"),
                    ),
                ),
            ),
        )
        val api = FakeGeminiImageApi(response = response)
        val store = FakeImageBytesStore(savedPath = "/cache/aistudio/generated.png")
        val decoder = FakeBase64Decoder(decoded = byteArrayOf(1, 2, 3))
        val generator = GeminiCardImageGenerator(api, store, decoder)

        val image = generator.generate("A neon-lit Tokyo street")

        assertEquals("A neon-lit Tokyo street", image.prompt)
        assertEquals("file:///cache/aistudio/generated.png", image.imageUri)
        assertEquals(listOf("encoded"), decoder.decodedValues)
        assertEquals(1, store.savedBytes.size)
        assertTrue(store.savedBytes.single().contentEquals(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun `response without an image block throws IOException`() = runBlocking {
        val response = InteractionResponseDto(
            id = "interaction-2",
            status = "completed",
            steps = listOf(
                InteractionStepDto(
                    type = "model_output",
                    content = listOf(InteractionContentDto(type = "text", text = "I can't create that.")),
                ),
            ),
        )
        val api = FakeGeminiImageApi(response = response)
        val store = FakeImageBytesStore(savedPath = "/cache/aistudio/generated.png")
        val decoder = FakeBase64Decoder(decoded = ByteArray(0))
        val generator = GeminiCardImageGenerator(api, store, decoder)

        try {
            generator.generate("A retro VHS cover")
            fail("Expected an IOException")
        } catch (expected: IOException) {
            assertTrue(store.savedBytes.isEmpty())
        }
    }

    @Test
    fun `api failure is propagated`() = runBlocking {
        val api = FakeGeminiImageApi(failure = IllegalStateException("boom"))
        val store = FakeImageBytesStore(savedPath = "/cache/aistudio/generated.png")
        val decoder = FakeBase64Decoder(decoded = ByteArray(0))
        val generator = GeminiCardImageGenerator(api, store, decoder)

        try {
            generator.generate("A lone figure on a red desert planet")
            fail("Expected the api failure to propagate")
        } catch (expected: IllegalStateException) {
            assertEquals("boom", expected.message)
        }
    }
}

private class FakeGeminiImageApi(
    private val response: InteractionResponseDto? = null,
    private val failure: Throwable? = null,
) : GeminiImageApi {

    override suspend fun createInteraction(request: InteractionRequestDto): InteractionResponseDto {
        failure?.let { throw it }
        return response ?: error("No response configured")
    }
}

private class FakeImageBytesStore(private val savedPath: String) : ImageBytesStore {
    val savedBytes = mutableListOf<ByteArray>()
    val deletedUris = mutableListOf<String>()

    override fun save(bytes: ByteArray): String {
        savedBytes += bytes
        return savedPath
    }

    override fun delete(uri: String) {
        deletedUris += uri
    }

    override fun clear() {
        cleared = true
    }

    var cleared = false
}

private class FakeBase64Decoder(private val decoded: ByteArray) : Base64Decoder {
    val decodedValues = mutableListOf<String>()

    override fun decode(value: String): ByteArray {
        decodedValues += value
        return decoded
    }
}
