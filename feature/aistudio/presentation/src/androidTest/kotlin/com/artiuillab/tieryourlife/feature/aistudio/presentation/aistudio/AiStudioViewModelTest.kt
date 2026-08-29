package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.CardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.GenerationOutcome
import com.artiuillab.tieryourlife.feature.aistudio.domain.library.GeneratedCardSaver
import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiStudioViewModelTest {

    @Test
    fun send_showsGeneratingThenResult() = runBlocking {
        val generator = FakeCardImageGenerator().apply { armGate() }
        val viewModel = viewModel(generator)

        viewModel.send("A neon street")
        generator.generateStarted.await()
        val generatingState = viewModel.state.first { it.exchanges.isNotEmpty() }
        assertEquals(AiExchangePhase.Generating, generatingState.exchanges.single().phase)
        assertEquals(true, generatingState.generating)

        generator.releaseGenerate()
        val resultState = viewModel.state.first { it.exchanges.single().phase is AiExchangePhase.Result }
        assertEquals(false, resultState.generating)
        val image = (resultState.exchanges.single().phase as AiExchangePhase.Result).image
        assertEquals("A neon street", image.prompt)
    }

    @Test
    fun openingTheStudio_sweepsLeftoverGeneratedFiles() = runBlocking {
        val generator = FakeCardImageGenerator()
        viewModel(generator)

        generator.awaitDiscardAll()

        assertEquals(1, generator.discardAllCount)
    }

    @Test
    fun send_whenGeneratorFails_showsFailed() = runBlocking {
        val generator = FakeCardImageGenerator().apply { shouldFail = true }
        val viewModel = viewModel(generator)

        viewModel.send("A neon street")
        val state = viewModel.state.first { it.exchanges.singleOrNull()?.phase == AiExchangePhase.Failed }

        assertEquals(false, state.generating)
    }

    @Test
    fun retry_afterFailure_succeedsWithTheSamePrompt() = runBlocking {
        val generator = FakeCardImageGenerator().apply { shouldFail = true }
        val viewModel = viewModel(generator)
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
        val viewModel = viewModel(generator)
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
    fun addToList_marksTheExchangeAsAdded_andDoesNotSaveTwice() = runBlocking {
        val generator = FakeCardImageGenerator()
        val saver = FakeGeneratedCardSaver()
        val viewModel = viewModel(generator, saver)
        viewModel.send("A neon street")
        val resultState = viewModel.state.first { it.exchanges.single().phase is AiExchangePhase.Result }
        val exchangeId = resultState.exchanges.single().id
        val image = (resultState.exchanges.single().phase as AiExchangePhase.Result).image

        viewModel.addToList(exchangeId, "My card")
        val addedState = viewModel.state.first { it.exchanges.single().addedItemId != null }

        assertEquals(1L, addedState.exchanges.single().addedItemId)
        assertEquals(listOf(Triple(1L, "My card", image.imageUri)), saver.calls)

        viewModel.addToList(exchangeId, "My card again")
        assertEquals(1, saver.calls.size)
    }

    @Test
    fun addedItemIds_returnsIdsInTheOrderTheyWereAdded() = runBlocking {
        val generator = FakeCardImageGenerator()
        val saver = FakeGeneratedCardSaver()
        val viewModel = viewModel(generator, saver)

        viewModel.send("First")
        val firstResult = viewModel.state.first { it.exchanges.singleOrNull()?.phase is AiExchangePhase.Result }
        val firstId = firstResult.exchanges.single().id
        viewModel.addToList(firstId, "First card")
        viewModel.state.first { it.exchanges.single().addedItemId != null }

        viewModel.send("Second")
        val secondResult = viewModel.state.first {
            it.exchanges.size == 2 && it.exchanges.last().phase is AiExchangePhase.Result
        }
        val secondId = secondResult.exchanges.last().id
        viewModel.addToList(secondId, "Second card")
        viewModel.state.first { it.exchanges.last().addedItemId != null }

        assertEquals(listOf(1L, 2L), viewModel.addedItemIds)
    }

    @Test
    fun regenerate_onAnAlreadyAddedExchange_isNoOp() = runBlocking {
        val generator = FakeCardImageGenerator()
        val saver = FakeGeneratedCardSaver()
        val viewModel = viewModel(generator, saver)
        viewModel.send("A neon street")
        val resultState = viewModel.state.first { it.exchanges.single().phase is AiExchangePhase.Result }
        val exchangeId = resultState.exchanges.single().id
        viewModel.addToList(exchangeId, "My card")
        viewModel.state.first { it.exchanges.single().addedItemId != null }
        val callCountBefore = generator.callCount

        viewModel.regenerate(exchangeId)

        assertEquals(callCountBefore, generator.callCount)
        assertEquals(1, saver.calls.size)
    }

    @Test
    fun openingTheStudio_showsTheBalanceItReads() = runBlocking {
        val viewModel = viewModel(credits = FakeGenerationCredits(balance = 7))

        val state = viewModel.state.first { it.credits != null }

        assertEquals(7, state.credits)
    }

    // The stub build draws its images locally and counts nothing, so the top
    // bar must show no number at all rather than a zero.
    @Test
    fun whenNothingIsCounted_theBalanceStaysAbsent() = runBlocking {
        val generator = FakeCardImageGenerator()
        val viewModel = viewModel(generator, credits = FakeGenerationCredits(balance = null))

        viewModel.send("A neon street")
        val state = viewModel.state.first { it.exchanges.single().phase is AiExchangePhase.Result }

        assertNull(state.credits)
    }

    @Test
    fun send_takesTheNewBalanceFromTheGenerationItself() = runBlocking {
        val generator = FakeCardImageGenerator().apply { creditsRemaining = 4 }
        val viewModel = viewModel(generator, credits = FakeGenerationCredits(balance = 5))
        viewModel.state.first { it.credits == 5 }

        viewModel.send("A neon street")
        val state = viewModel.state.first { it.exchanges.single().phase is AiExchangePhase.Result }

        assertEquals(4, state.credits)
    }

    @Test
    fun send_whenOutOfCredits_showsItsOwnCardAndAnEmptyBalance() = runBlocking {
        val generator = FakeCardImageGenerator().apply { outcome = GenerationOutcome.OutOfCredits }
        val viewModel = viewModel(generator, credits = FakeGenerationCredits(balance = 1))

        viewModel.send("A neon street")
        val state = viewModel.state.first {
            it.exchanges.singleOrNull()?.phase == AiExchangePhase.OutOfCredits
        }

        assertEquals(0, state.credits)
        assertEquals(false, state.generating)
    }

    // A refused generation is not a failed one: the backend never charged for
    // it, so the retry path must not be offered in its place.
    @Test
    fun outOfCredits_isNotReportedAsAFailure() = runBlocking {
        val generator = FakeCardImageGenerator().apply { outcome = GenerationOutcome.OutOfCredits }
        val viewModel = viewModel(generator)

        viewModel.send("A neon street")
        val state = viewModel.state.first { it.exchanges.singleOrNull()?.phase != AiExchangePhase.Generating }

        assertEquals(AiExchangePhase.OutOfCredits, state.exchanges.single().phase)
    }

    // The backend hands the credit back when generation fails, so the number
    // on screen must not drop for something nobody was charged for.
    @Test
    fun aFailedGeneration_leavesTheBalanceAlone() = runBlocking {
        val generator = FakeCardImageGenerator().apply { shouldFail = true }
        val viewModel = viewModel(generator, credits = FakeGenerationCredits(balance = 3))
        viewModel.state.first { it.credits == 3 }

        viewModel.send("A neon street")
        val state = viewModel.state.first { it.exchanges.singleOrNull()?.phase == AiExchangePhase.Failed }

        assertEquals(3, state.credits)
    }

    private fun viewModel(
        generator: CardImageGenerator = FakeCardImageGenerator(),
        saver: GeneratedCardSaver = FakeGeneratedCardSaver(),
        credits: GenerationCredits = FakeGenerationCredits(),
    ): AiStudioViewModel = AiStudioViewModel(generator, credits, saver, savedStateHandle())

    private fun savedStateHandle(): SavedStateHandle =
        SavedStateHandle(mapOf("tierListId" to 1L, "listTitle" to "Sci-fi films"))
}

private class FakeCardImageGenerator : CardImageGenerator {
    var shouldFail = false
    var outcome: GenerationOutcome? = null
    var creditsRemaining: Int? = null
    var callCount = 0
    var discardAllCount = 0
    val discarded = mutableListOf<GeneratedCardImage>()
    val generateStarted = CompletableDeferred<Unit>()

    private val discardAllCalled = CompletableDeferred<Unit>()
    private var generateGate: CompletableDeferred<Unit> = CompletableDeferred<Unit>().apply { complete(Unit) }

    fun armGate() {
        generateGate = CompletableDeferred()
    }

    fun releaseGenerate() {
        generateGate.complete(Unit)
    }

    override suspend fun generate(prompt: String): GenerationOutcome {
        callCount++
        if (!generateStarted.isCompleted) generateStarted.complete(Unit)
        generateGate.await()
        outcome?.let { return it }
        if (shouldFail) return GenerationOutcome.Failed
        return GenerationOutcome.Success(
            image = GeneratedCardImage(prompt = prompt, imageUri = "file:///cache/aistudio/$callCount.png"),
            creditsRemaining = creditsRemaining,
        )
    }

    override suspend fun discard(image: GeneratedCardImage) {
        discarded += image
    }

    override suspend fun discardAll() {
        discardAllCount++
        discardAllCalled.complete(Unit)
    }

    suspend fun awaitDiscardAll() = discardAllCalled.await()
}

private class FakeGenerationCredits(private val balance: Int? = null) : GenerationCredits {
    override fun lastKnown(): Int? = null

    override suspend fun remaining(): Int? = balance
}

private class FakeGeneratedCardSaver : GeneratedCardSaver {
    var nextId = 1L
    val calls = mutableListOf<Triple<Long, String, String>>()

    override suspend fun save(tierListId: Long, title: String, imageUri: String): Long {
        calls += Triple(tierListId, title, imageUri)
        return nextId++
    }
}
