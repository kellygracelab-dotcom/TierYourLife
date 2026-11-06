package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "No topic, said out loud" rides in the topic id's column, and never reads back as an id. */
class TopicTakenOffTest {

    @Test
    fun aTopicTakenOff_readsAsNoTopicThatSaysSo() {
        val board = TierListEntity(id = 1, title = "Films", topicId = TOPIC_TAKEN_OFF).toDomain()

        assertNull(board.topicId)
        assertTrue(board.topicTakenOff)
    }

    @Test
    fun aKnownTopic_andNone_readAsTheyAre() {
        val filed = TierListEntity(id = 1, title = "Films", topicId = "t1", topicName = "Oscars").toDomain()
        val none = TierListEntity(id = 1, title = "Films").toDomain()

        assertEquals("t1", filed.topicId)
        assertFalse(filed.topicTakenOff)
        assertNull(none.topicId)
        assertFalse(none.topicTakenOff)
    }
}
