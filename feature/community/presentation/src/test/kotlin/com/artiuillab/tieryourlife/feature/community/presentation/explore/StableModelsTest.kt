package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private var cardCompositions = 0
private var listCompositions = 0
private var lastTitle = ""

// Each reads its parameter: one a composable never reads is never compared.
@Composable
private fun CardProbe(summary: PublishedListSummary) {
    val title = summary.title
    SideEffect {
        lastTitle = title
        cardCompositions++
    }
}

@Composable
private fun ListProbe(lists: List<PublishedListSummary>) {
    val size = lists.size
    SideEffect { listCompositions += size }
}

/**
 * The server's answer after the kept page, or a page parsed again, is the same
 * cards as new objects. config/compose/stability.conf lets Compose see they
 * are equal and skip them, rather than drawing every card on screen again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StableModelsTest {

    @get:Rule
    val compose = createComposeRule()

    private val summary = PublishedListSummary(
        id = "1",
        title = "A list",
        authorUid = "u",
        authorName = "someone",
        category = "games",
        itemCount = 3,
        tierColors = listOf("#B03A32"),
        updatedAtMillis = 0,
    )

    @Test
    fun anEqualCardThatArrivesAgainIsNotDrawnAgain() {
        cardCompositions = 0
        listCompositions = 0
        var parentCompositions = 0
        val shown = mutableStateOf(listOf(summary), neverEqualPolicy())
        compose.setContent {
            val lists = shown.value
            SideEffect { parentCompositions++ }
            ListProbe(lists)
            CardProbe(lists.first())
        }
        compose.runOnIdle { shown.value = listOf(summary.copy(tierColors = listOf("#B03A32"))) }
        compose.waitForIdle()

        assertEquals(2, parentCompositions)
        assertEquals(1, cardCompositions)
        assertEquals(1, listCompositions)
    }
}
