package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.CardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.domain.library.GeneratedCardSaver
import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiStudioViewModelTest {

    @Test
    fun send_showsGeneratingThenResult() = runBlocking {
        val generator = FakeCardImageGenerator()
        val viewModel = AiStudioViewModel(generator, FakeGeneratedCardSaver(), savedStateHandle())

        viewModel.send("A neon street")
        val generatingState = viewModel.state.first { it.exchanges.isNotEmpty() }
        assertEquals(AiExchangePhase.Generating, generatingState.exchanges.single().phase)
        assertEquals(true, generatingState.generating)

        val resultState = viewModel.state.first { it.exchanges.single().phase is AiExchangePhase.Result }
        assertEquals(false, resultState.generating)
        val image = (resultState.exchanges.single().phase as AiExchangePhase.Result).image
        assertEquals("A neon street", image.prompt)
    }

    @Test
    fun send_whenGeneratorFails_showsFailed() = runBlocking {
        val generator = FakeCardImageGenerator().apply { shouldFail = true }
        val viewModel = AiStudioViewModel(generator, FakeGeneratedCardSaver(), savedStateHandle())

        viewModel.send("A neon street")
        val state = viewModel.state.first { it.exchanges.singleOrNull()?.phase == AiExchangePhase.Failed }

        assertEquals(false, state.generating)
    }

    @Test
    fun retry_afterFailure_succeedsWithTheSamePrompt() = runBlocking {
        val generator = FakeCardImageGenerator().apply { shouldFail = true }
        val viewModel = AiStudioViewModel(generator, FakeGeneratedCardSaver(), savedStateHandle())
        viewModel.send("A neon street")
        val failedState = viewModel.state.first { it.exchanges.singleOrNull()?.phase == AiExchangePhase.Failed }
        val exchangeId = failedState.exchanges.single().id

        generator.shouldFail = false
        viewModel.retry(exchangeId)
        val resultState = viewModel.state.first { it.exchanges.single().phase is AiExchangePhase.Result }

        assertEquals(exchangeId, resultState.exchanges.single().id)
        assertEquals(
            "A neon street",
            (resultState.exchanges.single().phase as AiExchangePhase.Result).image.prompt,
        )
    }

    @Test
    fun regenerate_discardsThePreviousImage_andReplacesTheSameCard() = runBlocking {
        val generator = FakeCardImageGenerator()
        val viewModel = AiStudioViewModel(generator, FakeGeneratedCardSaver(), savedStateHandle())
        viewModel.send("A neon street")
        val firstResult = viewModel.state.first { it.exchanges.single().phase is AiExchangePhase.Result }
        val exchangeId = firstResult.exchanges.single().id
        val firstImage = (firstResult.exchanges.single().phase as AiExchangePhase.Result).image

        viewModel.regenerate(exchangeId)
        val secondResult = viewModel.state.first {
            val phase = it.exchanges.single().phase
            phase is AiExchangePhase.Result && phase.image != firstImage
        }

        assertEquals(1, secondResult.exchanges.size)
        assertEquals(exchangeId, secondResult.exchanges.single().id)
        assertEquals(listOf(firstImage), generator.discarded)
    }

    @Test
    fun addToList_savesWithTheExchangesPromptImage_andInvokesTheCallback() = runBlocking {
        val generator = FakeCardImageGenerator()
        val saver = FakeGeneratedCardSaver()
        val viewModel = AiStudioViewModel(generator, saver, savedStateHandle())
        viewModel.send("A neon street")
        val resultState = viewModel.state.first { it.exchanges.single().phase is AiExchangePhase.Result }
        val exchangeId = resultState.exchanges.single().id
        val image = (resultState.exchanges.single().phase as AiExchangePhase.Result).image

        val added = CompletableDeferred<Long>()
        viewModel.addToList(exchangeId, "My card") { itemId -> added.complete(itemId) }
        val newItemId = added.await()

        assertEquals(1L, newItemId)
        assertEquals(listOf(Triple(1L, "My card", image.imageUri)), saver.calls)
    }

    private fun savedStateHandle(): SavedStateHandle =
        SavedStateHandle(mapOf("tierListId" to 1L, "listTitle" to "Sci-fi films"))
}

private class FakeCardImageGenerator : CardImageGenerator {
    var shouldFail = false
    var callCount = 0
    val discarded = mutableListOf<GeneratedCardImage>()

    override suspend fun generate(prompt: String): GeneratedCardImage {
        callCount++
        if (shouldFail) throw IOException("No active network")
        return GeneratedCardImage(prompt = prompt, imageUri = "file:///cache/aistudio/$callCount.png")
    }

    override suspend fun discard(image: GeneratedCardImage) {
        discarded += image
    }
}

private class FakeGeneratedCardSaver : GeneratedCardSaver {
    var nextId = 1L
    val calls = mutableListOf<Triple<Long, String, String>>()

    override suspend fun save(tierListId: Long, title: String, imageUri: String): Long {
        calls += Triple(tierListId, title, imageUri)
        return nextId++
    }
}
