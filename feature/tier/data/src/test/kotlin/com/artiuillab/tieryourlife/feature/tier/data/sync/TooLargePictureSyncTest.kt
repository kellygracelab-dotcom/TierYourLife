package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The account's folder takes under 4 MB: a picture that size is refused every time, so it is not sent. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class TooLargePictureSyncTest : SyncEngineBench() {

    private class Recording : Pictures {
        val sent = mutableListOf<String>()

        override suspend fun put(pictureId: String, bytes: ByteArray): Boolean {
            sent += pictureId
            return true
        }

        override suspend fun get(pictureId: String): ByteArray? = null
    }

    private suspend fun coverOf(uid: String, cover: String) {
        database.tierDao().setCover(dao.boardByUid(uid)!!.id, cover, null)
    }

    @Test
    fun aPictureOfFourMegabytes_isNotSent() = runBlocking {
        coverOf(givenLocalBoard("Films"), images.write("large", ByteArray(4 * 1024 * 1024)))
        coverOf(givenLocalBoard("Books"), images.write("small", byteArrayOf(1, 2, 3)))
        val vault = Recording()

        engine(vault = vault, connection = Unmetered).sync()

        assertEquals(listOf("small"), vault.sent)
    }
}
