package com.artiuillab.tieryourlife.feature.tier.data.local.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.feature.tier.data.di.DatabaseModule
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * QA A13: a cover kept as the gallery's own address lost its permission with
 * the process that picked it. It is no cover, and the cards' art decides.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class GalleryLinkCoverTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun aCoverLeftAsAGalleryAddress_isNoCoverOnceTheLibraryOpens() = runBlocking {
        val first = DatabaseModule.provideTierDatabase(context)
        val dao = first.boardSyncDao()
        dao.insertBoard(TierListEntity(title = "Gallery", uid = "gallery", coverImageUrl = "content://media/picker/0/42"))
        dao.insertBoard(TierListEntity(title = "Web", uid = "web", coverImageUrl = "https://i/cover.jpg"))
        dao.insertBoard(TierListEntity(title = "Own", uid = "own", coverImageUrl = "/data/user/0/app/files/tier_images/cover-1"))
        first.close()

        val second = DatabaseModule.provideTierDatabase(context)
        try {
            val reopened = second.boardSyncDao()
            assertNull(reopened.boardByUid("gallery")!!.coverImageUrl)
            assertEquals("https://i/cover.jpg", reopened.boardByUid("web")!!.coverImageUrl)
            assertEquals("/data/user/0/app/files/tier_images/cover-1", reopened.boardByUid("own")!!.coverImageUrl)
        } finally {
            second.close()
        }
    }
}
