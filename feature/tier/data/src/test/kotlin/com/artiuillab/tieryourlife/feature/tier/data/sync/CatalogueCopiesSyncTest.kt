package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.IncomingTier
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toCoverImageUrls
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private const val POSTER = "https://image.tmdb.org/t/p/w500/x.jpg"
private const val DINO = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/72/Stego.jpg/500px-Stego.jpg"
private const val LOCAL_WIKI = "https://upload.wikimedia.org/wikipedia/en/a/a1/Logo.png"
private const val OURS = "https://firebasestorage.googleapis.com/v0/b/tieryourlife.firebasestorage.app/o/published%2Fcatalogue%2Fw-"
private const val POSTER_COPY = "${OURS}aa?alt=media"
private const val DINO_COPY = "${OURS}bb?alt=media"
private const val LOCAL_WIKI_COPY = "${OURS}cc?alt=media"

/** Proxy #126: the account names the shared copies it now shows, and the board here takes them as an ordinary edit. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CatalogueCopiesSyncTest : SyncEngineBench() {

    @Test
    fun copiesNamedByTheAccount_replaceTheCatalogueAddresses_andGoBackUpAsAnEdit() = runBlocking {
        val credited = PictureAttribution(source = "wikimedia", author = "Jane Doe", license = "CC BY-SA 4.0")
        val uid = givenBoardWith(
            TierItemEntity(tierId = 0, position = 0, title = "Alien", imageUrl = POSTER),
            TierItemEntity(tierId = 0, position = 1, title = "Stegosaurus", imageUrl = DINO, attribution = credited),
            TierItemEntity(tierId = 0, position = 2, title = "Logo", imageUrl = LOCAL_WIKI),
            TierItemEntity(tierId = 0, position = 3, title = "Mine", imageUrl = null),
            cover = POSTER,
            collage = listOf(POSTER, DINO, "https://example.org/other.jpg", LOCAL_WIKI),
        )
        boards.copies = mapOf(POSTER to POSTER_COPY, DINO to DINO_COPY, LOCAL_WIKI to LOCAL_WIKI_COPY)
        val engine = engine()

        engine.sync()

        val board = dao.boardByUid(uid)!!
        val items = dao.itemsOf(board.id).sortedBy { it.position }
        assertEquals(listOf(POSTER_COPY, DINO_COPY, LOCAL_WIKI_COPY, null), items.map { it.imageUrl })
        assertEquals(PictureAttribution(source = "tmdb", originalUrl = POSTER), items[0].attribution)
        assertEquals(credited.copy(originalUrl = DINO), items[1].attribution)
        // No catalogue the account knows: no credit is made up, as on the account.
        assertNull(items[2].attribution)
        assertEquals(POSTER_COPY, board.coverImageUrl)
        assertEquals(listOf(POSTER_COPY, DINO_COPY, "https://example.org/other.jpg", LOCAL_WIKI_COPY), board.coverImageUrls.toCoverImageUrls())

        val (first, second) = boards.kept.map { it.second }
        assertNull(first.basedOn)
        assertEquals(1, second.basedOn)
        assertEquals(listOf(POSTER_COPY, DINO_COPY, LOCAL_WIKI_COPY, null), second.items.sortedBy { it.position }.map { it.imageUrl })
        assertEquals(POSTER, second.items.single { it.position == 0 }.attribution?.originalUrl)
        assertEquals(fingerprintOf(uid), boards.server.getValue(uid).fingerprint)
        assertEquals(2, boards.server.getValue(uid).revision)

        // One board, nothing left to send, and the next run sends nothing.
        assertEquals(1, dao.boardsOf(someone).size)
        assertEquals(emptySet<String>(), engine.unsent())
        engine.sync()
        assertEquals(2, boards.kept.size)
        assertEquals(1, dao.boardsOf(someone).size)
    }

    @Test
    fun aBoardEditedWhileItWentUp_keepsTheEdit_andTakesTheCopiesWithItsOwnWrite() = runBlocking {
        val uid = givenBoardWith(TierItemEntity(tierId = 0, position = 0, title = "Alien", imageUrl = POSTER))
        boards.copies = mapOf(POSTER to POSTER_COPY)
        var edited = false
        boards.onKeep = {
            if (!edited) {
                edited = true
                runBlocking { dao.renameBoard(uid, "Films, renamed") }
            }
        }
        val engine = engine()

        engine.sync()

        val board = dao.boardByUid(uid)!!
        assertEquals("Films, renamed", board.title)
        assertEquals(POSTER, dao.itemsOf(board.id).single().imageUrl)
        assertEquals(1, boards.kept.size)

        engine.sync()

        val after = dao.boardByUid(uid)!!
        assertEquals(POSTER_COPY, dao.itemsOf(after.id).single().imageUrl)
        assertEquals(listOf(null, 1, 2), boards.kept.map { it.second.basedOn })
        assertEquals("Films, renamed", boards.server.getValue(uid).title)
        assertEquals(fingerprintOf(uid), boards.server.getValue(uid).fingerprint)
        assertEquals(1, dao.boardsOf(someone).size)
        assertEquals(emptySet<String>(), engine.unsent())
    }

    @Test
    fun noCopies_noSecondWrite() = runBlocking {
        val uid = givenBoardWith(TierItemEntity(tierId = 0, position = 0, title = "Alien", imageUrl = POSTER))

        engine().sync()

        assertEquals(1, boards.kept.size)
        assertEquals(POSTER, dao.itemsOf(dao.boardByUid(uid)!!.id).single().imageUrl)
    }

    @Test
    fun catalogues_areNamedAsTheAccountNamesThem() {
        assertEquals("tmdb", catalogueSourceOf(POSTER))
        assertEquals("wikimedia", catalogueSourceOf(DINO))
        assertEquals("igdb", catalogueSourceOf("https://images.igdb.com/igdb/image/upload/t_cover_big/coqa1.jpg"))
        assertNull(catalogueSourceOf(LOCAL_WIKI))
        assertNull(catalogueSourceOf("http://image.tmdb.org/t/p/w500/x.jpg"))
        assertNull(catalogueSourceOf("https://image.tmdb.org.evil.test/x.jpg"))
        assertNull(catalogueSourceOf("not an address"))
    }

    private suspend fun givenBoardWith(vararg items: TierItemEntity, cover: String? = null, collage: List<String> = emptyList()): String {
        val board = TierListEntity(
            title = "Films",
            ownerUid = someone,
            coverImageUrl = cover,
            coverImageUrls = collage.joinToString("\n").ifEmpty { null },
        )
        dao.addBoard(
            board,
            listOf(
                IncomingTier(
                    tier = TierEntity(tierListId = 0, position = 0, label = "S", colorLight = "#111111", colorDark = "#EEEEEE"),
                    items = items.toList(),
                ),
            ),
        )
        return board.uid
    }
}
