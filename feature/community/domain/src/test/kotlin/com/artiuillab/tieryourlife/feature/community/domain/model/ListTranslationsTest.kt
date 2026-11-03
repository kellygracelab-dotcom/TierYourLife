package com.artiuillab.tieryourlife.feature.community.domain.model

import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ListTranslationsTest {

    private val ghibli = ListTranslations(
        mapOf(
            "ru" to mapOf("Studio Ghibli Movies" to "Фильмы студии Ghibli", "Spirited Away" to "Унесённые призраками", "Must watch" to "Обязательно"),
            "pt-BR" to mapOf("Spirited Away" to "A Viagem de Chihiro"),
            "de" to mapOf("Spirited Away" to " "),
        ),
    )

    @Test
    fun eachTranslatedLanguage_isFoundByItsLanguageWhateverTheRegion() {
        assertEquals("ru", translationLocale("ru"))
        assertEquals("ru", translationLocale("ru-RU"))
        assertEquals("uk", translationLocale("uk-UA"))
        assertEquals("ja", translationLocale("ja-JP"))
        assertEquals("ar", translationLocale("ar-EG"))
        assertEquals("tr", translationLocale("tr_TR"))
        assertEquals(listOf("ru", "uk", "de", "es", "fr", "ja", "pl", "pt-BR", "tr", "ar"), TRANSLATED_LOCALES.toList())
    }

    @Test
    fun portuguese_isTranslatedOnlyForBrazil() {
        assertEquals("pt-BR", translationLocale("pt-BR"))
        assertEquals("pt-BR", translationLocale("pt-br"))
        assertEquals("pt-BR", translationLocale("pt-Latn-BR"))
        assertNull(translationLocale("pt-PT"))
        assertNull(translationLocale("pt"))
        assertNull(translationLocale("pt-PT-u-rg-brzzzz"))
    }

    @Test
    fun englishAndUntranslatedLanguages_readTheListAsWritten() {
        assertNull(translationLocale("en"))
        assertNull(translationLocale("en-US"))
        assertNull(translationLocale("it-IT"))
        assertNull(translationLocale("zh-Hans-CN"))
        assertNull(translationLocale("br"))
        assertNull(translationLocale(""))
        assertNull(translationLocale("und"))
    }

    @Test
    fun t_isTheTranslation_orTheTextAsWritten() {
        assertEquals("Унесённые призраками", ghibli.t("ru-RU", "Spirited Away"))
        assertEquals("A Viagem de Chihiro", ghibli.t("pt-BR", "Spirited Away"))
        assertEquals("Spirited Away", ghibli.t("pt-PT", "Spirited Away"))
        assertEquals("Spirited Away", ghibli.t("en-US", "Spirited Away"))
        assertEquals("My Neighbor Totoro", ghibli.t("ru", "My Neighbor Totoro"))
        assertEquals("Spirited Away", ghibli.t("fr", "Spirited Away"))
    }

    @Test
    fun aBlankTranslation_saysNothing() {
        assertEquals("Spirited Away", ghibli.t("de", "Spirited Away"))
    }

    @Test
    fun aSummaryTitle_isItsNameInTheReadersLanguage_orItsOwn() {
        val summary = PublishedListSummary(
            id = "NI9W1YK6aNpeDfz4DQQb",
            title = "Studio Ghibli Movies",
            authorUid = "tyl-editorial",
            authorName = "",
            category = "anime",
            itemCount = 2,
            updatedAtMillis = 0,
            names = mapOf("ru" to "Фильмы студии Ghibli"),
        )

        assertEquals("Фильмы студии Ghibli", summary.titleIn("ru-RU"))
        assertEquals("Studio Ghibli Movies", summary.titleIn("uk"))
        assertEquals("Studio Ghibli Movies", summary.titleIn("en"))
        assertEquals("Studio Ghibli Movies", summary.copy(names = emptyMap()).titleIn("ru"))
    }

    @Test
    fun aBoard_readsInTheLanguage_titleTiersAndCards_butThePoolKeepsItsName() {
        val board = TierList(
            id = 0,
            title = "Studio Ghibli Movies",
            tiers = listOf(
                Tier(id = 0, label = "Must watch", caption = "Must watch", colorLight = "#000000", colorDark = "#FFFFFF", items = listOf(TierItem(1, "Spirited Away", null))),
                Tier(id = -1, label = "Must watch", colorLight = "#000000", colorDark = "#FFFFFF", items = listOf(TierItem(2, "Ponyo", null)), isPool = true),
            ),
        )

        val read = board.readIn(ghibli.wordsIn("ru"))

        assertEquals("Фильмы студии Ghibli", read.title)
        assertEquals("Обязательно", read.tiers[0].label)
        assertEquals("Обязательно", read.tiers[0].caption)
        assertEquals("Унесённые призраками", read.tiers[0].items.single().title)
        assertEquals("Must watch", read.tiers[1].label)
        assertEquals("Ponyo", read.tiers[1].items.single().title)
        assertSame(board, board.readIn(ghibli.wordsIn("en")))
    }
}
