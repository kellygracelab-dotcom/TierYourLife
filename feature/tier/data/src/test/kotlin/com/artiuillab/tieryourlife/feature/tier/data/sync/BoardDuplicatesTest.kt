package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.IncomingTier
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.BoardsApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeepBoardRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardRevisionDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptItemDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.IOException

private const val POSTER = "https://image.tmdb.org/t/p/w500/x.jpg"
private const val POSTER_COPY = "https://firebasestorage.googleapis.com/v0/b/tieryourlife.firebasestorage.app/o/published%2Fcatalogue%2Fw-aa?alt=media"

/**
 * Danylo, 25.11: a second board whenever the phone and the site both had one
 * open. Backend's logs, 26.11: seven on the phone since 01.11, none on the
 * site; one of them with only the phone writing.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class BoardDuplicatesTest : SyncEngineBench() {

    @Test
    fun twoWritesInARow_withNobodyElseWriting_makeNoSecondBoard() = runBlocking {
        val uid = givenBoard("Alien", "Doom")
        val account = TheAccount()
        engine(api = account).sync()

        editedHere(uid, "Doom", "Doom II")
        engine(api = account).sync()
        editedHere(uid, "Alien", "Aliens")
        engine(api = account).sync()

        assertEquals(1, dao.boardsOf(someone).size)
        assertEquals(listOf("Aliens", "Doom II"), titlesHere(uid))
        assertEquals(listOf("Aliens", "Doom II"), titlesThere(uid))
    }

    // 24.11, "TV series I've watched": the account named our own write with
    // what it kept for us, at our own revision, and an edit here made it a copy.
    @Test
    fun ourOwnWriteComingBackWithWhatTheAccountKept_isNoSecondBoard() = runBlocking {
        val uid = givenBoard("Alien", "Doom")
        val account = TheAccount()
        engine(api = account).sync()
        val stored = boards.server.getValue(uid)
        boards.server[uid] = stored.copy(fingerprint = "state-v1:kept-for-us", items = stored.items.map { it.copy(sourceIndex = it.position) })

        editedHere(uid, "Doom", "Doom II")
        engine(api = account).sync()

        assertEquals(1, dao.boardsOf(someone).size)
        assertEquals(listOf("Alien", "Doom II"), titlesHere(uid))
        // What the account kept for us is kept here too, and goes back up with the edit.
        assertEquals(listOf(0, 1), itemsHere(uid).map { it.sourceIndex })
        assertEquals(stored.revision, boards.kept.last().second.basedOn)
    }

    @Test
    fun copiesWhoseAnswerNeverCame_makeNoSecondBoard() = runBlocking {
        val uid = givenBoard("Alien", "Doom", picture = POSTER)
        boards.copies = mapOf(POSTER to POSTER_COPY)
        runCatching { engine(api = TheAccount(loseAnswer = 2)).sync() }

        editedHere(uid, "Doom", "Doom II")
        engine(api = TheAccount()).sync()

        assertEquals(1, dao.boardsOf(someone).size)
        assertEquals(listOf("Alien", "Doom II"), titlesHere(uid))
        assertEquals(POSTER_COPY, itemsHere(uid).first().imageUrl)
    }

    @Test
    fun theSiteEditsOneCard_thePhoneAnother_oneBoardWithBoth() = runBlocking {
        val uid = givenBoard("Alien", "Doom")
        val account = TheAccount()
        engine(api = account).sync()

        editedOnTheSite(uid) { if (it.title == "Alien") it.copy(title = "Alien, renamed on the site") else it }
        editedHere(uid, "Doom", "Doom, renamed here")
        engine(api = account).sync()

        assertEquals(1, dao.boardsOf(someone).size)
        assertEquals(listOf("Alien, renamed on the site", "Doom, renamed here"), titlesHere(uid))
        assertEquals(listOf("Alien, renamed on the site", "Doom, renamed here"), titlesThere(uid))
        assertEquals(boards.server.getValue(uid).revision - 1, boards.kept.last().second.basedOn)
        // Agreed again: the next run sends nothing.
        val writes = boards.kept.size
        engine(api = account).sync()
        assertEquals(writes, boards.kept.size)
    }

    @Test
    fun theSiteReordersATier_thePhoneRenamesACard_bothStay() = runBlocking {
        val uid = givenBoard("Alien", "Doom", "Quake")
        val account = TheAccount()
        engine(api = account).sync()

        editedOnTheSite(uid) { it.copy(position = 2 - it.position) }
        editedHere(uid, "Doom", "Doom II")
        engine(api = account).sync()

        assertEquals(1, dao.boardsOf(someone).size)
        assertEquals(listOf("Quake", "Doom II", "Alien"), titlesHere(uid))
    }

    @Test
    fun theSameCardEditedOnBothSides_stillKeepsBoth() = runBlocking {
        val uid = givenBoard("Alien", "Doom")
        val account = TheAccount()
        engine(api = account).sync()

        editedOnTheSite(uid) { if (it.title == "Doom") it.copy(title = "Doom on the site") else it }
        editedHere(uid, "Doom", "Doom here")
        engine(api = account).sync()

        assertEquals(2, dao.boardsOf(someone).size)
        assertEquals(listOf("Alien", "Doom here"), titlesHere(uid))
    }

    /** As the account: a read gives back whole what a write sent. [loseAnswer]: that write is kept, its answer lost. */
    private inner class TheAccount(private val loseAnswer: Int? = null) : BoardsApi by boards {
        private var writes = 0

        override suspend fun keep(uid: String, request: KeepBoardRequestDto): Response<KeptBoardRevisionDto> {
            val answer = boards.keep(uid, request)
            if (answer.isSuccessful) {
                boards.server[uid] = boards.server.getValue(uid).copy(tiers = request.tiers, items = request.items)
            }
            writes++
            if (writes == loseAnswer) throw IOException("The answer never came")
            return answer
        }
    }

    private suspend fun givenBoard(vararg titles: String, picture: String? = null): String {
        val board = TierListEntity(title = "Films", ownerUid = someone)
        dao.addBoard(
            board,
            listOf(
                IncomingTier(
                    tier = TierEntity(tierListId = 0, position = 0, label = "S", colorLight = "#111111", colorDark = "#EEEEEE"),
                    items = titles.mapIndexed { index, title ->
                        TierItemEntity(tierId = 0, position = index, title = title, imageUrl = picture.takeIf { index == 0 })
                    },
                ),
            ),
        )
        return board.uid
    }

    private fun editedOnTheSite(uid: String, edit: (KeptItemDto) -> KeptItemDto) {
        val stored = boards.server.getValue(uid)
        boards.server[uid] = stored.copy(revision = stored.revision + 1, fingerprint = "site-${stored.revision + 1}", items = stored.items.map(edit))
    }

    private suspend fun editedHere(uid: String, title: String, renamed: String) {
        val item = itemsHere(uid).single { it.title == title }
        dao.updateItem(item.copy(title = renamed))
    }

    private suspend fun itemsHere(uid: String) = dao.itemsOf(dao.boardByUid(uid)!!.id).sortedBy { it.position }

    private suspend fun titlesHere(uid: String) = itemsHere(uid).map { it.title }

    private fun titlesThere(uid: String) = boards.server.getValue(uid).items.sortedBy { it.position }.map { it.title }
}
