package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedIdDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListSummaryDto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Proxy #207, 26.11: `awaitingApproval: true` while a new list waits for approval before the feed, absent otherwise. */
class AwaitingApprovalAnswerTest {

    @Test
    fun `a publish answer says whether the list waits for approval`() {
        val waiting = networkJson.decodeFromString(PublishedIdDto.serializer(), """{"id":"p-1","awaitingApproval":true}""")
        val notWaiting = networkJson.decodeFromString(PublishedIdDto.serializer(), """{"id":"p-1"}""")

        assertTrue(waiting.toPublished("sent").awaitingApproval)
        assertFalse(notWaiting.toPublished("sent").awaitingApproval)
    }

    @Test
    fun `one's own lists say which wait for approval`() {
        val mine = """{"id":"p-1","title":"Films","authorName":"Ann","itemCount":3,"updatedAt":1,"awaitingApproval":true}"""

        assertTrue(networkJson.decodeFromString(PublishedListSummaryDto.serializer(), mine).toSummary().awaitingApproval)
    }
}
