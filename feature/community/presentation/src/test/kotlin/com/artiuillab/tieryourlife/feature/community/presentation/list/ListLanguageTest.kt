package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.feature.community.domain.model.ListTranslations
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** The page is drawn in the reader's language, and what comes back from the screen is kept as written. */
class ListLanguageTest {

    private val board = TierList(
        id = 0,
        title = "Studio Ghibli Movies",
        tiers = listOf(
            Tier(id = 0, label = "Masterpiece", colorLight = "#B03A32", colorDark = "#F1948C", items = listOf(TierItem(0, "Spirited Away", null))),
            pool(listOf(TierItem(1, "Ponyo", null))),
        ),
    )
    private val translations = ListTranslations(
        mapOf("ru" to mapOf("Studio Ghibli Movies" to "Фильмы студии Ghibli", "Masterpiece" to "Шедевр", "Spirited Away" to "Унесённые призраками", "Ponyo" to "Рыбка Поньо")),
    )
    private val page = CommunityListUiState.Success(list = board, authorName = "", translations = translations)

    @Test
    fun everyBoardOnThePage_readsInTheReadersLanguage() {
        val read = page.readIn("ru-RU") as CommunityListUiState.Success

        listOf(read.list, read.mine, read.theirs).forEach { shown ->
            assertEquals("Фильмы студии Ghibli", shown.title)
            assertEquals(listOf("Шедевр", "Unranked"), shown.tiers.map { it.label })
            assertEquals(listOf("Унесённые призраками", "Рыбка Поньо"), shown.tiers.flatMap { it.items }.map { it.title })
        }
    }

    @Test
    fun aReaderOfTheListsOwnLanguage_getsThePageAsItIs() {
        assertSame(page, page.readIn("en-US"))
        assertSame(page, page.readIn("pt-PT"))
        assertSame(CommunityListUiState.Loading, CommunityListUiState.Loading.readIn("ru"))
    }

    @Test
    fun aGamesResultReadInRussian_isKeptInTheListsOwnWords() {
        val read = (page.readIn("ru") as CommunityListUiState.Success).mine
        val played = read.copy(tiers = listOf(read.tiers[0].copy(items = read.tiers.flatMap { it.items }), read.tiers[1].copy(items = emptyList())))

        val kept = played.inWordsOf(page.mine, page.list)

        assertEquals("Studio Ghibli Movies", kept.title)
        assertEquals("Masterpiece", kept.tiers[0].label)
        assertEquals(listOf("Spirited Away", "Ponyo"), kept.tiers[0].items.map { it.title })
        assertEquals(listOf(0L, 1L), kept.tiers[0].items.map { it.id })
    }
}
