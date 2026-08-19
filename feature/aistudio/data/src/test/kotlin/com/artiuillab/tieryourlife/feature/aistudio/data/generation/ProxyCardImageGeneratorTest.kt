package com.artiuillab.tieryourlife.feature.aistudio.data.generation

import com.artiuillab.tieryourlife.feature.aistudio.data.remote.api.CardImageApi
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.GenerateImageRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ProxyCardImageGeneratorTest {

    @Test
    fun `saves the bytes the proxy returned and points the card at the file`() = runBlocking {
        val bytes = byteArrayOf(1, 2, 3)
        val api = FakeCardImageApi(body = bytes)
        val store = FakeImageBytesStore(savedPath = "/data/cache/card.jpg")
        val generator = ProxyCardImageGenerator(api, store)

        val image = generator.generate("a neon-lit Tokyo street")

        assertEquals("a neon-lit Tokyo street", api.requests.single().prompt)
        assertArrayEquals(bytes, store.savedBytes.single())
        assertEquals("file:///data/cache/card.jpg", image.imageUri)
        assertEquals("a neon-lit Tokyo street", image.prompt)
    }

    @Test
    fun `fails when the proxy returns an empty body`() = runBlocking {
        val api = FakeCardImageApi(body = ByteArray(0))
        val store = FakeImageBytesStore(savedPath = "/data/cache/card.jpg")
        val generator = ProxyCardImageGenerator(api, store)

        val result = runCatching { generator.generate("anything") }

        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(store.savedBytes.isEmpty())
    }

    @Test
    fun `lets a transport failure through instead of saving something broken`() = runBlocking {
        val api = FakeCardImageApi(failure = IllegalStateException("boom"))
        val store = FakeImageBytesStore(savedPath = "/data/cache/card.jpg")
        val generator = ProxyCardImageGenerator(api, store)

        val result = runCatching { generator.generate("anything") }

        assertTrue(result.isFailure)
        assertTrue(store.savedBytes.isEmpty())
    }

    @Test
    fun `discard removes only the image it was given`() = runBlocking {
        val api = FakeCardImageApi(body = byteArrayOf(1))
        val store = FakeImageBytesStore(savedPath = "/data/cache/card.jpg")
        val generator = ProxyCardImageGenerator(api, store)

        val image = generator.generate("anything")
        generator.discard(image)

        assertEquals(listOf(image.imageUri), store.deletedUris)
        assertTrue(!store.cleared)
    }
}

private class FakeCardImageApi(
    private val body: ByteArray? = null,
    private val failure: Throwable? = null,
) : CardImageApi {
    val requests = mutableListOf<GenerateImageRequestDto>()

    override suspend fun generate(request: GenerateImageRequestDto): ResponseBody {
        requests += request
        failure?.let { throw it }
        return (body ?: ByteArray(0)).toResponseBody("image/jpeg".toMediaType())
    }
}

private class FakeImageBytesStore(private val savedPath: String) : ImageBytesStore {
    val savedBytes = mutableListOf<ByteArray>()
    val deletedUris = mutableListOf<String>()
    var cleared = false

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
}
