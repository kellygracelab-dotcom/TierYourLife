package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.storage.StorageException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PictureDownloadFailureTest {

    @Test
    fun aServerError_isReportedByItsCodesAlone() {
        val error = StorageException.fromExceptionAndHttpCode(IOException("users/uid-1/pictures/p-1 refused"), 503)!!

        val failure = downloadFailureOf(error)!!

        assertEquals("Storage answered HTTP 503, error ${error.errorCode}", failure.message)
        assertNull(failure.cause)
    }

    @Test
    fun aPictureNotUploadedYet_isAnOrdinaryMiss() {
        assertNull(downloadFailureOf(StorageException.fromExceptionAndHttpCode(IOException("not found"), 404)!!))
    }

    @Test
    fun noAnswerAtAll_isNoConnection_notAFailure() {
        assertNull(downloadFailureOf(StorageException.fromExceptionAndHttpCode(IOException("Unable to resolve host"), 0)!!))
    }
}
