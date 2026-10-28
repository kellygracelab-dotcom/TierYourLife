package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.SectionDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.SectionsDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ThemePairDto
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionLookup
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStatus
import com.artiuillab.tieryourlife.feature.community.domain.model.ThemePair
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** What the sections answers become on the phone, from today's server and from the one docs/sections-api.md describes. */
class SectionMappingTest {

    @Test
    fun `the answer as the draft writes it reads field for field`() {
        val json = """
            {"sections":[{"id":"street-food","name":"Street food","names":{"uk":"Вулична їжа"},"icon":"local_cafe",
            "pictureUrl":"https://firebasestorage.googleapis.com/x","tint":{"light":"#7A4A33","dark":"#3A2A26"},
            "topicBg":{"light":"#F3E6DF","dark":"#2B2220"},"topicIcon":{"light":"#7A4A33","dark":"#E8C3B0"},
            "status":"approved","builtIn":false,"rank":null,"approvedAt":1793146000000,"listCount":41,"lists30d":12,
            "newThisWeek":3,"followerCount":9,"following":true,"collage":["https://a","https://b"]}],"updatedAt":1793146553000}
        """.trimIndent()

        val section = sectionsFrom(networkJson.decodeFromString(SectionsDto.serializer(), json).sections).single()

        assertEquals("street-food", section.id)
        assertEquals("Вулична їжа", section.nameIn("uk"))
        assertEquals("Street food", section.nameIn("de"))
        assertEquals("local_cafe", section.icon)
        assertEquals("https://firebasestorage.googleapis.com/x", section.pictureUrl)
        assertEquals(ThemePair("#7A4A33", "#3A2A26"), section.tint)
        assertEquals(ThemePair("#7A4A33", "#E8C3B0"), section.topicIcon)
        assertEquals(1_793_146_000_000L, section.approvedAt)
        assertEquals(12, section.lists30d)
        assertTrue(section.following)
        assertFalse(section.builtIn)
    }

    @Test
    fun `today's answer, counts and nothing else, still gives the nine with their own icons`() {
        val answer = listOf(SectionDto(id = "anime", listCount = 5), SectionDto(id = "other"), SectionDto(id = "food", newThisWeek = 22))

        val sections = sectionsFrom(answer)

        assertEquals(listOf("anime", "food", "other"), sections.map { it.id })
        assertEquals("animation", sections.first().icon)
        assertTrue(sections.all { it.builtIn && it.status == SectionStatus.Approved && it.tint == null && it.name.isEmpty() })
        assertEquals(22, sections[1].newThisWeek)
    }

    @Test
    fun `other comes last whatever order the server sent`() {
        val sections = sectionsFrom(listOf(SectionDto(id = "other"), SectionDto(id = "games"), SectionDto(id = "cars")))

        assertEquals(listOf("games", "cars", "other"), sections.map { it.id })
    }

    @Test
    fun `an empty answer leaves the nine standing`() {
        assertEquals(Section.BUILT_IN, sectionsFrom(emptyList()))
    }

    @Test
    fun `an icon the app cannot draw falls back to the built-in's, or to category`() {
        val sections = sectionsFrom(listOf(SectionDto(id = "games", icon = "rocket"), SectionDto(id = "cars", icon = "rocket")))

        assertEquals(listOf("sports_esports", "category"), sections.map { it.icon })
    }

    @Test
    fun `a built-in never carries a picture, and nobody carries one that is not on the web`() {
        val sections = sectionsFrom(
            listOf(SectionDto(id = "food", pictureUrl = "https://x"), SectionDto(id = "cars", pictureUrl = "http://x")),
        )

        assertTrue(sections.all { it.pictureUrl == null })
    }

    @Test
    fun `a colour that does not read as one is no colour, and Other's look stands in`() {
        val section = sectionsFrom(listOf(SectionDto(id = "cars", tint = ThemePairDto("#7A4A33", "dark"), topicBg = ThemePairDto("#FFF", "#000000")))).single()

        assertNull(section.tint)
        assertNull(section.topicBg)
    }

    @Test
    fun `hidden and waiting sections are kept as such, for the screens to leave out`() {
        val sections = sectionsFrom(listOf(SectionDto(id = "cars", hidden = true), SectionDto(id = "boats", status = "pending", fileUnder = "sport")))

        assertFalse(sections[0].shown)
        assertFalse(sections[1].shown)
        assertEquals("sport", sections[1].fileUnder)
    }

    @Test
    fun `a proposal is this person's own only when they sent it, and an approved section is nobody's`() {
        val answer = listOf(
            SectionDto(id = "kites", status = "pending", proposerUid = "me", fileUnder = "sport", waitingLists = 2),
            SectionDto(id = "boats", status = "pending", proposerUid = "somebody"),
            SectionDto(id = "noodles", status = "declined", proposerUid = "me", declineNote = "Too close to Food"),
            SectionDto(id = "cars", status = "approved", proposerUid = "me"),
        )

        val sections = sectionsFrom(answer, me = "me")

        assertEquals(listOf(true, false, true, false), sections.map { it.mine })
        assertEquals(listOf("kites"), sections.filter { it.waiting }.map { it.id })
        assertEquals("Too close to Food", sections[2].declineNote)
        assertEquals(2, sections[0].waitingLists)
        assertFalse(sectionsFrom(answer, me = null).any { it.mine })
    }

    @Test
    fun `the collage keeps four web pictures at most`() {
        val pictures = listOf("https://a", "http://b", "https://c", "https://d", "https://e", "https://f")

        val games = sectionsFrom(listOf(SectionDto(id = "games", collage = pictures))).single()

        assertEquals(listOf("https://a", "https://c", "https://d", "https://e"), games.collage)
    }

    @Test
    fun `a section kept on the phone reads back as it was sent`() {
        val section = sectionsFrom(
            listOf(SectionDto(id = "cars", name = "Cars", names = mapOf("uk" to "Авто"), tint = ThemePairDto("#111111", "#222222"), rank = 2, lists30d = 4)),
        ).single()

        assertEquals(section, section.toDto().toDomain())
    }

    @Test
    fun `one section reads bare or wrapped`() {
        val bare = networkJson.decodeFromString(JsonObject.serializer(), """{"id":"cars","name":"Cars"}""")
        val wrapped = networkJson.decodeFromString(JsonObject.serializer(), """{"section":{"id":"cars","name":"Cars"}}""")

        assertEquals("Cars", sectionFrom(bare).name)
        assertEquals("Cars", sectionFrom(wrapped).name)
    }

    @Test
    fun `not found is an answer, no such section`() {
        assertEquals(SectionLookup.Unknown, refusal(404, """{"code":"NOT_FOUND"}""").sectionRefusal())
    }

    @Test
    fun `gone with somewhere to go is a merge`() {
        assertEquals(SectionLookup.Merged("food"), refusal(410, """{"code":"MERGED","mergedInto":"food"}""").sectionRefusal())
    }

    @Test
    fun `gone without saying where is as good as unknown`() {
        assertEquals(SectionLookup.Unknown, refusal(410, "<html>").sectionRefusal())
    }

    @Test
    fun `any other failure is no answer at all`() {
        assertNull(refusal(500, "").sectionRefusal())
        assertNull(IOException("offline").sectionRefusal())
    }

    private fun refusal(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))
}
