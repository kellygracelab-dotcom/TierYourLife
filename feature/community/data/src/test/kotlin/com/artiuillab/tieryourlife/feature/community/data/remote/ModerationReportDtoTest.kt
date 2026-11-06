package com.artiuillab.tieryourlife.feature.community.data.remote

import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ModerationReportDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModerationReportDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun anonymousRow_carriesNoAuthor() {
        val row = json.decodeFromString<ModerationReportDto>(
            """{"listId":"1","authorName":"","authorUid":null,"authorPhotoUrl":null,"anonymous":true}""",
        )

        assertTrue(row.anonymous)
        assertNull(row.authorUid)
    }

    @Test
    fun olderProxy_withoutTheField_isNotAnonymous() {
        assertFalse(json.decodeFromString<ModerationReportDto>("""{"listId":"1","authorName":"Taras"}""").anonymous)
    }
}
