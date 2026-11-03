package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.cache.decodeOpened
import com.artiuillab.tieryourlife.feature.community.data.cache.decodePage
import com.artiuillab.tieryourlife.feature.community.data.cache.encodeOpened
import com.artiuillab.tieryourlife.feature.community.data.cache.encodePage
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedFeedDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.ListTranslations
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** A house list's names and translations reach the screens, kept ones included, and their absence is none. */
class TranslationsMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val ghibli = """{"id":"NI9W1YK6aNpeDfz4DQQb","title":"Studio Ghibli Movies","authorUid":"tyl-editorial","authorName":"","anonymous":true,""" +
        """"names":{"ru":"Фильмы студии Ghibli"},"tiers":[{"label":"S","colorLight":"#B03A32","colorDark":"#F1948C"}],""" +
        """"items":[{"title":"Spirited Away","tierIndex":0}],""" +
        """"translations":{"ru":{"Studio Ghibli Movies":"Фильмы студии Ghibli","Spirited Away":"Унесённые призраками"}}}"""

    @Test
    fun `a summary carries the title's names, and none when the server sends none`() {
        val page = json.decodeFromString(
            PublishedFeedDto.serializer(),
            """{"lists":[{"id":"a","title":"Studio Ghibli Movies","authorName":"","itemCount":1,"names":{"ru":"Фильмы студии Ghibli","pt-BR":"Filmes do Studio Ghibli"}},""" +
                """{"id":"b","title":"B","authorName":"X","itemCount":1,"names":{}},""" +
                """{"id":"c","title":"C","authorName":"X","itemCount":1}]}""",
        )

        assertEquals(mapOf("ru" to "Фильмы студии Ghibli", "pt-BR" to "Filmes do Studio Ghibli"), page.lists[0].toSummary().names)
        assertEquals(emptyMap<String, String>(), page.lists[1].toSummary().names)
        assertEquals(emptyMap<String, String>(), page.lists[2].toSummary().names)
    }

    @Test
    fun `an opened list carries its translations and the title's names`() {
        val list = json.decodeFromString(PublishedListDto.serializer(), ghibli).toDomain()

        assertEquals("Унесённые призраками", list.translations.t("ru", "Spirited Away"))
        assertEquals(mapOf("ru" to "Фильмы студии Ghibli"), list.summary.names)
    }

    @Test
    fun `a list without translations has none`() {
        val list = json.decodeFromString(PublishedListDto.serializer(), """{"id":"l","title":"Films","authorName":"Olena"}""").toDomain()

        assertEquals(ListTranslations(), list.translations)
        assertEquals(emptyMap<String, String>(), list.summary.names)
    }

    @Test
    fun `kept lists and kept feeds read back in every language they had`() {
        val list = json.decodeFromString(PublishedListDto.serializer(), ghibli).toDomain()

        assertEquals(list, decodeOpened(encodeOpened(list, keptBy = null), me = null))
        assertEquals(list.summary.names, decodePage(encodePage(CommunityPage(lists = listOf(list.summary)))).lists.single().names)
    }
}
