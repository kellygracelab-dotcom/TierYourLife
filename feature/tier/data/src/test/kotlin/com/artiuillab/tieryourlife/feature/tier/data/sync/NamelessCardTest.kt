package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.IncomingTier
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A31: `/boards` takes a nameless card with a picture it can keep; one with neither still needs a name. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class NamelessCardTest : SyncEngineBench() {

    private suspend fun givenCard(title: String, imageUrl: String?): String {
        val board = TierListEntity(title = "Holiday", ownerUid = someone)
        dao.addBoard(
            board,
            listOf(
                IncomingTier(
                    tier = TierEntity(tierListId = 0, position = 0, label = "S", colorLight = "#111111", colorDark = "#EEEEEE"),
                    items = listOf(TierItemEntity(tierId = 0, position = 0, title = title, imageUrl = imageUrl)),
                ),
            ),
        )
        return board.uid
    }

    private suspend fun titleHere(uid: String) = dao.itemsOf(dao.boardByUid(uid)!!.id).single().title

    @Test
    fun aNamelessCardWithAWebPicture_goesUpNameless() = runBlocking {
        val uid = givenCard("", imageUrl = "https://image.tmdb.org/t/p/w500/beach.jpg")

        engine().sync()

        assertEquals("", boards.kept.single().second.items.single().title)
        assertEquals("", titleHere(uid))
    }

    @Test
    fun aCardOfOnlySpaces_withAPicture_isNamelessHereAsInTheAccount() = runBlocking {
        val uid = givenCard("   ", imageUrl = "https://image.tmdb.org/t/p/w500/beach.jpg")

        engine().sync()

        assertEquals("", boards.kept.single().second.items.single().title)
        assertEquals("", titleHere(uid))
        assertEquals(fingerprintOf(uid), boards.kept.single().second.fingerprint)
    }

    // Neither a name nor a picture another device can fetch: refused nameless,
    // so it takes the words the site shows, here as well as in the account.
    @Test
    fun aCardWithNeither_isNamedTheSameHereAndInTheAccount() = runBlocking {
        val uid = givenCard("", imageUrl = null)

        engine().sync()

        assertEquals(KeptText.UNTITLED_CARD, boards.kept.single().second.items.single().title)
        assertEquals(KeptText.UNTITLED_CARD, titleHere(uid))
        assertEquals(fingerprintOf(uid), boards.kept.single().second.fingerprint)
    }

    @Test
    fun aNamelessCardFromTheAccount_staysNameless() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Trip", revision = 1, pictureId = "beach").let { board ->
            board.copy(items = board.items.map { it.copy(title = "") })
        }

        engine().sync()

        assertEquals("", titleHere("remote-1"))
    }

    @Test
    fun aKeptPicture_isOursByIdOrAWebAddress() {
        assertTrue(KeptText.isKeptPicture("beach", null))
        assertTrue(KeptText.isKeptPicture(null, " HTTPS://example.com/a.jpg"))
        assertFalse(KeptText.isKeptPicture(null, "/data/user/0/files/a.jpg"))
        assertFalse(KeptText.isKeptPicture(null, null))
    }

    // A39: `/boards` drops an address this long, which would leave the card with neither.
    @Test
    fun aWebAddressTooLongForTheAccount_isNoKeptPicture() {
        val longest = "https://" + "a".repeat(KeptText.URL - "https://".length)

        assertTrue(KeptText.isKeptPicture(null, longest))
        assertFalse(KeptText.isKeptPicture(null, longest + "a"))
    }

    @Test
    fun aNamelessCardWithAnAddressTooLong_goesUpUntitled() = runBlocking {
        givenCard("", imageUrl = "https://example.com/" + "a".repeat(KeptText.URL))

        engine().sync()

        assertEquals(KeptText.UNTITLED_CARD, boards.kept.single().second.items.single().title)
    }
}
