package com.artiuillab.tieryourlife.feature.tier.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UnnamedCardTest {

    @Test
    fun theOldDefaultOnACardWithAPicture_readsAsNoName() {
        assertEquals("", cardTitleAsRead("Untitled card", hasPicture = true))
    }

    @Test
    fun aCardWithoutAPicture_keepsItsWords() {
        assertEquals("Untitled card", cardTitleAsRead("Untitled card", hasPicture = false))
    }

    @Test
    fun aRealName_isLeftAlone_evenOneThatOnlyLooksLikeTheDefault() {
        assertEquals("Untitled card 2", cardTitleAsRead("Untitled card 2", hasPicture = true))
        assertEquals("Dune", cardTitleAsRead("Dune", hasPicture = true))
    }
}
