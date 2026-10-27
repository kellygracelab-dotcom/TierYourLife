package com.artiuillab.tieryourlife.navigation

import androidx.compose.runtime.saveable.SaverScope
import com.artiuillab.tieryourlife.feature.tier.presentation.cover.CoverHandOffSubject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The hand-off from the cover to the main screen (§9): kept across the
 * activity being made again on unfold, dropped when cancelled or too late,
 * and opened in the right place.
 */
class HandOffTest {

    private val scope = SaverScope { true }

    private fun saveAndRestore(handOff: HandOff?): HandOff? {
        val saved = with(HandOffSaver) { scope.save(handOff) } ?: return null
        return HandOffSaver.restore(saved)
    }

    private val ownList = HandOff(CoverHandOffSubject.OwnList(listId = 4, title = "Every A24 film", tierId = 12), startedAtMillis = 1_000)
    private val wholeList = HandOff(CoverHandOffSubject.OwnList(listId = 4, title = "Every A24 film", tierId = null), startedAtMillis = 1_000)
    private val card = HandOff(CoverHandOffSubject.Card(listId = 4, tierId = 12, title = "Moonlight"), startedAtMillis = 2_000)
    private val community = HandOff(CoverHandOffSubject.CommunityList(publishedId = "pub-7", title = "PS2 era games"), startedAtMillis = 3_000)
    private val app = HandOff(CoverHandOffSubject.App, startedAtMillis = 4_000)

    @Test
    fun everyKindOfHandOff_comesBackAsItWasSaved() {
        listOf(ownList, wholeList, card, community, app).forEach { handOff ->
            assertEquals(handOff, saveAndRestore(handOff))
        }
    }

    @Test
    fun aCancelledHandOff_comesBackAsNothing() {
        assertNull(saveAndRestore(null))
    }

    @Test
    fun somethingUnrecognised_isDropped_notACrash() {
        assertNull(handOffFromSaved(listOf("teleport", 1_000L)))
        assertNull(handOffFromSaved(listOf("card", 1_000L, 4L, "Moonlight")))
        assertNull(handOffFromSaved(emptyList()))
    }

    @Test
    fun theWait_lastsThirtySeconds() {
        assertEquals(30_000L, HandOffRules.remainingMillis(ownList, nowMillis = 1_000))
        assertEquals(12_000L, HandOffRules.remainingMillis(ownList, nowMillis = 19_000))
        assertEquals(0L, HandOffRules.remainingMillis(ownList, nowMillis = 31_000))
        assertEquals(0L, HandOffRules.remainingMillis(ownList, nowMillis = 90_000))
    }

    @Test
    fun aClockThatWentBack_stillWaitsNoLongerThanThirtySeconds() {
        assertEquals(30_000L, HandOffRules.remainingMillis(ownList, nowMillis = 0))
    }

    @Test
    fun aPhoneOpenedInTime_getsTheHandOff() {
        assertEquals(ownList, HandOffRules.stillWanted(ownList, nowMillis = 30_999))
    }

    @Test
    fun aPhoneOpenedTooLate_getsNothing() {
        assertNull(HandOffRules.stillWanted(ownList, nowMillis = 31_000))
        assertNull(HandOffRules.stillWanted(null, nowMillis = 0))
    }

    @Test
    fun yourList_opensItsBoard_atTheTierTheCoverShowed() {
        assertEquals(HandOffDestination.Board(listId = 4, focusTierId = 12), HandOffRules.destinationOf(ownList.subject))
        assertEquals(HandOffDestination.Board(listId = 4, focusTierId = null), HandOffRules.destinationOf(wholeList.subject))
    }

    @Test
    fun aCard_opensItsBoard_atItsTier() {
        assertEquals(HandOffDestination.Board(listId = 4, focusTierId = 12), HandOffRules.destinationOf(card.subject))
    }

    @Test
    fun aCommunityList_opensItsOwnPage() {
        assertEquals(HandOffDestination.CommunityList(publishedId = "pub-7", title = "PS2 era games"), HandOffRules.destinationOf(community.subject))
    }

    @Test
    fun theAppItself_staysOnTheLists() {
        assertEquals(HandOffDestination.Home, HandOffRules.destinationOf(app.subject))
    }
}
