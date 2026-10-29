package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/** A change pulled from the account replaces what it carries and nothing else. */
class PulledBoardTest {

    private val here = TierListEntity(
        id = 7,
        uid = "b-1",
        title = "Films",
        publishedFingerprint = "sent-once",
        topicId = "t-1",
        topicName = "Sci-fi",
        newTopicName = "Space operas",
        takenFrom = "pub-9",
        rankingOf = "pub-3",
        arrivedFrom = "Pixel 7",
        favouritedAt = 1_793_146_000_000,
    )

    private val pulled = TierListEntity(id = 7, uid = "b-1", title = "Films, renamed on the web", anonymous = true)

    @Test
    fun `the star and the other fields only this phone keeps survive a pulled change`() {
        val kept = pulled.keepingLocalFieldsOf(here)

        assertEquals(1_793_146_000_000, kept.favouritedAt)
        assertEquals("sent-once", kept.publishedFingerprint)
        assertEquals("t-1", kept.topicId)
        assertEquals("Sci-fi", kept.topicName)
        assertEquals("Space operas", kept.newTopicName)
        assertEquals("pub-9", kept.takenFrom)
        assertEquals("pub-3", kept.rankingOf)
        assertEquals("Pixel 7", kept.arrivedFrom)
    }

    @Test
    fun `what the account carries comes from the account`() {
        val kept = pulled.keepingLocalFieldsOf(here)

        assertEquals("Films, renamed on the web", kept.title)
        assertEquals(true, kept.anonymous)
    }
}
