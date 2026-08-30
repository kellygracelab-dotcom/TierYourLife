package com.artiuillab.tieryourlife.feature.aistudio.data.generation

import com.artiuillab.tieryourlife.feature.aistudio.data.remote.api.CardImageApi
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.AdoptGuestCreditsRequestDto
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.AdoptedCreditsDto
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.CreditsResponseDto
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.GenerateImageRequestDto
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.GenerationOutcome
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ProxyCardImageGeneratorTest {

    @Test
    fun `saves the bytes the proxy returned and points the card at the file`() = runBlocking {
        val bytes = byteArrayOf(1, 2, 3)
        val api = FakeCardImageApi(body = bytes)
        val store = FakeImageBytesStore(savedPath = "/data/cache/card.jpg")
        val generator = ProxyCardImageGenerator(api, store)

        val outcome = generator.generate("a neon-lit Tokyo street")

        assertEquals("a neon-lit Tokyo street", api.requests.single().prompt)
        assertArrayEquals(bytes, store.savedBytes.single())
        val success = outcome as GenerationOutcome.Success
        assertEquals("file:///data/cache/card.jpg", success.image.imageUri)
        assertEquals("a neon-lit Tokyo street", success.image.prompt)
    }

    // The balance rides back on the image, so the studio can show the new count
    // without asking a second time.
    @Test
    fun `reads the balance left from the response header`() = runBlocking {
        val api = FakeCardImageApi(body = byteArrayOf(1), creditsHeader = "4")
        val generator = ProxyCardImageGenerator(api, FakeImageBytesStore("/data/cache/card.jpg"))

        val outcome = generator.generate("anything")

        assertEquals(4, (outcome as GenerationOutcome.Success).creditsRemaining)
    }

    @Test
    fun `a missing balance header is not an error`() = runBlocking {
        val api = FakeCardImageApi(body = byteArrayOf(1))
        val generator = ProxyCardImageGenerator(api, FakeImageBytesStore("/data/cache/card.jpg"))

        val outcome = generator.generate("anything")

        assertNull((outcome as GenerationOutcome.Success).creditsRemaining)
    }

    // 402 is the one refusal that must not read as a failure: trying again
    // would be refused identically, and the screen says something different.
    @Test
    fun `reports an empty balance separately from a failure`() = runBlocking {
        val api = FakeCardImageApi(errorCode = 402)
        val store = FakeImageBytesStore(savedPath = "/data/cache/card.jpg")
        val generator = ProxyCardImageGenerator(api, store)

        val outcome = generator.generate("anything")

        assertEquals(GenerationOutcome.OutOfCredits, outcome)
        assertTrue(store.savedBytes.isEmpty())
    }

    @Test
    fun `any other refusal is a plain failure`() = runBlocking {
        val api = FakeCardImageApi(errorCode = 503)
        val generator = ProxyCardImageGenerator(api, FakeImageBytesStore("/data/cache/card.jpg"))

        assertEquals(GenerationOutcome.Failed, generator.generate("anything"))
    }

    @Test
    fun `fails when the proxy returns an empty body`() = runBlocking {
        val api = FakeCardImageApi(body = ByteArray(0))
        val store = FakeImageBytesStore(savedPath = "/data/cache/card.jpg")
        val generator = ProxyCardImageGenerator(api, store)

        val outcome = generator.generate("anything")

        assertEquals(GenerationOutcome.Failed, outcome)
        assertTrue(store.savedBytes.isEmpty())
    }

    @Test
    fun `a transport failure is reported without saving something broken`() = runBlocking {
        val api = FakeCardImageApi(failure = IOException("boom"))
        val store = FakeImageBytesStore(savedPath = "/data/cache/card.jpg")
        val generator = ProxyCardImageGenerator(api, store)

        val outcome = generator.generate("anything")

        assertEquals(GenerationOutcome.Failed, outcome)
        assertTrue(store.savedBytes.isEmpty())
    }

    @Test
    fun `discard removes only the image it was given`() = runBlocking {
        val api = FakeCardImageApi(body = byteArrayOf(1))
        val store = FakeImageBytesStore(savedPath = "/data/cache/card.jpg")
        val generator = ProxyCardImageGenerator(api, store)

        val image = (generator.generate("anything") as GenerationOutcome.Success).image
        generator.discard(image)

        assertEquals(listOf(image.imageUri), store.deletedUris)
        assertTrue(!store.cleared)
    }
}

private class FakeCardImageApi(
    private val body: ByteArray? = null,
    private val creditsHeader: String? = null,
    private val errorCode: Int? = null,
    private val failure: Throwable? = null,
) : CardImageApi {
    val requests = mutableListOf<GenerateImageRequestDto>()

    override suspend fun generate(request: GenerateImageRequestDto): Response<ResponseBody> {
        requests += request
        failure?.let { throw it }
        errorCode?.let {
            return Response.error(it, """{"error":"nope"}""".toResponseBody("application/json".toMediaType()))
        }
        val headers = creditsHeader
            ?.let { Headers.headersOf("X-Credits-Remaining", it) }
            ?: Headers.headersOf()
        return Response.success((body ?: ByteArray(0)).toResponseBody("image/jpeg".toMediaType()), headers)
    }

    override suspend fun credits(): CreditsResponseDto = CreditsResponseDto(credits = 0)

    override suspend fun adoptGuestCredits(request: AdoptGuestCreditsRequestDto): AdoptedCreditsDto =
        AdoptedCreditsDto()
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
