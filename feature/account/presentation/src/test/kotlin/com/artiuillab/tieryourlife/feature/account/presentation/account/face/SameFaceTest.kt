package com.artiuillab.tieryourlife.feature.account.presentation.account.face

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SameFaceTest {

    // QA D16 and the unticked "Your photo": one file, under another token and encoding.
    @Test
    fun oneStorageFile_isOneFace_whateverTheTokenOrEncoding() {
        val slot = "https://firebasestorage.googleapis.com/v0/b/app/o/avatars%2Fann%2Fp1?alt=media&token=aaa"
        val profile = "https://firebasestorage.googleapis.com/v0/b/app/o/avatars/ann/p1?alt=media&token=bbb"

        assertTrue(sameFace(slot, profile))
        assertTrue(sameFace(null, null))
        assertFalse(sameFace(slot, null))
        assertFalse(sameFace(slot, "https://firebasestorage.googleapis.com/v0/b/app/o/avatars%2Fann%2Fp2?alt=media&token=aaa"))
    }

    // QA D18, 589: the upload's answer gives the site's public address, the slot Storage's own.
    @Test
    fun theSitesPublicAddress_andStoragesOwn_areOneFace() {
        val slot = "https://firebasestorage.googleapis.com/v0/b/tieryourlife.firebasestorage.app/o/avatars%2Fann%2Fp1?alt=media&token=aaa"
        val answered = "https://tieryourlife.web.app/img/original/avatars/ann/p1"

        assertTrue(sameFace(slot, answered))
        assertFalse(sameFace(answered, "https://tieryourlife.web.app/img/original/avatars/ann/p2"))
        assertFalse(sameFace(slot, "https://example.test/avatars/ann/p1"))
    }
}
