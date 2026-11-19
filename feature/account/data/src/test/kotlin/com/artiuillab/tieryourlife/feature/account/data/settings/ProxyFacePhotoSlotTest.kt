package com.artiuillab.tieryourlife.feature.account.data.settings

import com.artiuillab.tieryourlife.feature.account.data.remote.ColourDto
import com.artiuillab.tieryourlife.feature.account.data.remote.ColourWriteDto
import com.artiuillab.tieryourlife.feature.account.data.remote.FacePhotoApi
import com.artiuillab.tieryourlife.feature.account.data.remote.PhotoSlotDto
import com.artiuillab.tieryourlife.feature.account.data.remote.RemovedPhotoDto
import com.artiuillab.tieryourlife.feature.account.data.remote.RestoredPhotoDto
import com.artiuillab.tieryourlife.feature.account.data.remote.SettingsApi
import com.artiuillab.tieryourlife.feature.account.data.remote.SettingsDto
import com.artiuillab.tieryourlife.feature.account.domain.repository.RemovedPhoto
import com.artiuillab.tieryourlife.feature.account.domain.repository.RestoredPhoto
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** CONTRACT.md, lists: the account's photo, DELETE /lists/face and POST /lists/face/restore. */
class ProxyFacePhotoSlotTest {

    private class Server(var photo: PhotoSlotDto? = null, var restoreRefusal: Int? = null) : SettingsApi, FacePhotoApi {
        override suspend fun settings() = SettingsDto(photo = photo)

        override suspend fun keepColour(colour: ColourWriteDto): ColourDto = error("not asked")

        override suspend fun removePhoto(): RemovedPhotoDto {
            photo = null
            return RemovedPhotoDto(wasWorn = true)
        }

        override suspend fun restorePhoto(): RestoredPhotoDto {
            restoreRefusal?.let { throw HttpException(Response.error<Any>(it, "{}".toResponseBody())) }
            return RestoredPhotoDto(PhotoSlotDto("p1", PHOTO, 1), wasWorn = true)
        }
    }

    @Test
    fun theSlot_isThePhotosAddress_orNothing() = runBlocking {
        assertEquals(PHOTO, ProxyFacePhotoSlot(Server(PhotoSlotDto("p1", PHOTO, 1)), Server()).photo().getOrThrow())
        assertEquals(null, ProxyFacePhotoSlot(Server(), Server()).photo().getOrThrow())
    }

    @Test
    fun removing_saysWhetherItWasWorn_andUndoPutsItBack() = runBlocking {
        val server = Server(PhotoSlotDto("p1", PHOTO, 1))
        val slot = ProxyFacePhotoSlot(server, server)

        assertEquals(RemovedPhoto(wasWorn = true), slot.remove().getOrThrow())
        assertEquals(RestoredPhoto.Back(PHOTO, wasWorn = true), slot.restore().getOrThrow())
    }

    @Test
    fun undo_tooLate_isGone_andAfterANewUpload_isThatOne() = runBlocking {
        assertEquals(RestoredPhoto.Gone, ProxyFacePhotoSlot(Server(), Server(restoreRefusal = 404)).restore().getOrThrow())
        val replaced = Server(PhotoSlotDto("p2", NEW_PHOTO, 2), restoreRefusal = 409)
        assertEquals(RestoredPhoto.Replaced(NEW_PHOTO), ProxyFacePhotoSlot(replaced, replaced).restore().getOrThrow())
        assertTrue(ProxyFacePhotoSlot(Server(), Server(restoreRefusal = 500)).restore().isFailure)
    }

    private companion object {
        const val PHOTO = "https://example.test/avatars/u/p1"
        const val NEW_PHOTO = "https://example.test/avatars/u/p2"
    }
}
