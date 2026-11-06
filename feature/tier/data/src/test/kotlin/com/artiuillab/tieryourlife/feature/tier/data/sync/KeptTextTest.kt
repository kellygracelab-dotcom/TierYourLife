package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupRefusal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeptTextTest {

    @Test
    fun `whitespace collapses and trims as the account does`() {
        assertEquals("Blade Runner", KeptText.clean("  Blade \t\n Runner ", KeptText.TITLE))
    }

    @Test
    fun `the unicode spaces JavaScript counts collapse too`() {
        assertEquals("Blade Runner", KeptText.clean("Blade 　Runner﻿", KeptText.TITLE))
    }

    @Test
    fun `text is cut at the limit`() {
        assertEquals("a".repeat(KeptText.TITLE), KeptText.clean("a".repeat(KeptText.TITLE + 30), KeptText.TITLE))
    }

    @Test
    fun `nothing left is no text at all`() {
        assertNull(KeptText.clean(" \n ", KeptText.CAPTION))
    }

    // Past 80 the account cut the suffix off, and with it the one thing
    // telling the two boards apart.
    @Test
    fun `a suffix that tells two boards apart survives a long title`() {
        val renamed = withSuffix("a".repeat(KeptText.TITLE), "(from this phone)")

        assertEquals(KeptText.TITLE, renamed.length)
        assertEquals(true, renamed.endsWith(" (from this phone)"))
    }

    @Test
    fun `a short title keeps all of itself`() {
        assertEquals("Films (from this phone)", withSuffix("Films", "(from this phone)"))
    }

    @Test
    fun `a refusal reads back as it was written, title and all`() {
        val refusal = BackupRefusal("Films: the\nsequel", BackupRefusal.Reason.TooManyBoards)

        assertEquals(refusal, backupRefusalOf(refusal.encoded()))
    }

    @Test
    fun `a refusal from a later version reads as none`() {
        assertNull(backupRefusalOf("SomethingNew\nFilms"))
    }
}
