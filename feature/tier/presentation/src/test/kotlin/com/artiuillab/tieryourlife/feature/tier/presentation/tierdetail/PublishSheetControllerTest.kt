package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CoverKind
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.PublishSheetController
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.coverKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The publish sheet (Danylo, 16.11.2026): section, topic, Publish; what is chosen stays on the board. */
@OptIn(ExperimentalCoroutinesApi::class)
class PublishSheetControllerTest {

    @Test
    fun publishWithNoSection_pointsAtTheSection_andSendsNothing() = runTest {
        val h = Harness(this)
        h.sheet.open()
        h.sheet.publish()
        assertTrue(h.sheet.state.value!!.sectionWanted)
        assertEquals(0, h.sent)
    }

    @Test
    fun aSection_goesOnTheBoardAtOnce_andBringsItsTopics() = runTest {
        val h = Harness(this)
        h.sheet.open()
        h.sheet.publish()
        h.sheet.chooseSection("film")
        advanceUntilIdle()

        assertEquals(listOf(1L to "film"), h.boards.categories)
        assertEquals(listOf("film"), h.community.asked)
        val open = h.sheet.state.value!!
        assertFalse(open.sectionWanted)
        assertEquals(listOf("A24"), open.topics!!.map { it.name })

        h.sheet.publish()
        assertEquals(1, h.sent)
    }

    @Test
    fun anotherSection_takesOffTheTopicChosenUnderTheLast() = runTest {
        val h = Harness(this, board().copy(category = "film", topicId = "t1", topicName = "A24"))
        h.sheet.open()
        h.sheet.chooseSection("games")
        advanceUntilIdle()
        assertEquals(listOf(listOf(1L, null, null, null)), h.boards.topics)
    }

    @Test
    fun aTopic_andAProposedOne_areKeptOnTheBoard() = runTest {
        val h = Harness(this, board().copy(category = "film"))
        h.sheet.open()
        h.sheet.chooseTopic(A24)
        h.sheet.proposeTopic("A24 horror")
        h.sheet.chooseTopic(null)
        advanceUntilIdle()
        assertEquals(
            listOf(listOf(1L, "t1", "A24", null), listOf(1L, null, null, "A24 horror"), listOf(1L, null, null, null)),
            h.boards.topics,
        )
    }

    @Test
    fun closed_itIsGone_andOpenedAgainAsksForTheSectionsTopicsAfresh() = runTest {
        val h = Harness(this, board().copy(category = "film"))
        h.sheet.open()
        advanceUntilIdle()
        h.sheet.close()
        assertNull(h.sheet.state.value)
        h.sheet.open()
        advanceUntilIdle()
        assertEquals(listOf("film", "film"), h.community.asked)
    }

    @Test
    fun outToTheCoverPicker_itStepsAside_andComesBackAsItWas() = runTest {
        val h = Harness(this, board().copy(category = "film"))
        h.sheet.open()
        advanceUntilIdle()
        h.sheet.leaveForCover()
        assertTrue(h.sheet.state.value!!.away)
        h.sheet.back()
        val back = h.sheet.state.value!!
        assertFalse(back.away)
        assertEquals(listOf("A24"), back.topics!!.map { it.name })
    }

    @Test
    fun theCover_isNamedByWhatItIsMadeOf() {
        val withPicture = board().let { b -> b.copy(tiers = b.tiers.map { t -> t.copy(items = t.items.map { it.copy(imageUrl = "https://x/moonlight.jpg") }) }) }
        assertEquals(CoverKind.Colours, board().coverKind())
        assertEquals(CoverKind.Auto, withPicture.coverKind())
        assertEquals(CoverKind.Collage, withPicture.copy(coverImageUrls = listOf("https://a", "https://b")).coverKind())
        assertEquals(CoverKind.CardPicture, withPicture.copy(coverImageUrl = "https://x/moonlight.jpg").coverKind())
        assertEquals(CoverKind.Photo, withPicture.copy(coverImageUrl = "file:///covers/1.jpg").coverKind())
    }

    private class Harness(scope: CoroutineScope, var visible: TierList = board()) {
        val boards = RecordingTierRepository()
        val community = Topics()
        var sent = 0
        val sheet = PublishSheetController(
            boards, community, scope,
            current = { visible },
            reload = {
                boards.categories.lastOrNull()?.let { (_, category) -> visible = visible.copy(category = category) }
            },
            publish = { sent++ },
        )
    }

    private class Topics : FakeCommunityRepository() {
        val asked = mutableListOf<String?>()
        override suspend fun topics(query: String?, section: String?): Result<List<Topic>> {
            asked += section
            return Result.success(listOf(A24))
        }
    }

    private companion object {
        val A24 = Topic("t1", "A24", "film", TopicStatus.Approved)

        fun board() = TierList(
            1, "A24 films",
            listOf(
                Tier(10, "S", "#E8A0A0", "#824848", listOf(TierItem(1, "Moonlight", null))),
                Tier(11, "Unranked", "#DDDDDD", "#444444", emptyList(), isPool = true),
            ),
        )
    }
}
