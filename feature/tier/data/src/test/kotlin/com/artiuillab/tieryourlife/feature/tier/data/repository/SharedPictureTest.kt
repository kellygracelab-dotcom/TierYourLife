package com.artiuillab.tieryourlife.feature.tier.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.data.sync.SyncEngineBench
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

/** A board copied on this phone shares its pictures with the original: a new picture on one deleted the other's file. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SharedPictureTest : SyncEngineBench() {

    private val store by lazy { TierImageStore(folder.root, openSource = { uri -> ByteArrayInputStream(uri.toByteArray()) }) }
    private val repository by lazy { RoomTierRepository(database.tierDao(), store, owner = { "ann" }) { 0L } }

    private lateinit var shared: File

    private suspend fun cardsSharingAPicture(): Pair<Long, Long> {
        shared = File(store.copyToInternalStorage("content://poster"))
        val (original, copy) = listOf(givenLocalBoard("Films", owner = "ann"), givenLocalBoard("Films", owner = "bob"))
            .map { uid -> dao.itemsOf(dao.boardByUid(uid)!!.id).single() }
        dao.setItemImage(original.uid, shared.path)
        dao.setItemImage(copy.uid, shared.path)
        return original.id to copy.id
    }

    @Test
    fun aNewPictureOnTheCopy_leavesTheOriginalsFile() = runBlocking {
        val (_, copy) = cardsSharingAPicture()

        repository.attachImageToItem(copy, "content://new")

        assertTrue(shared.exists())
    }

    @Test
    fun aNewPictureOnTheOriginal_leavesTheCopysFile() = runBlocking {
        val (original, _) = cardsSharingAPicture()

        repository.attachImageToItem(original, "content://new")

        assertTrue(shared.exists())
    }

    @Test
    fun theFileGoes_withTheLastCardShowingIt() = runBlocking {
        val (original, copy) = cardsSharingAPicture()

        repository.attachImageToItem(original, "content://new")
        repository.attachImageToItem(copy, "content://newer")

        assertFalse(shared.exists())
    }
}
