package com.artiuillab.tieryourlife.feature.tier.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class PictureMetadataTest {

    // The rules let images through and nothing else; bytes sent with no type
    // arrive as an octet stream, and every board picture was turned away.
    @Test
    fun aPictureGoesUpAsTheImageItIs() {
        assertEquals("image/jpeg", pictureMetadata().contentType)
    }
}
