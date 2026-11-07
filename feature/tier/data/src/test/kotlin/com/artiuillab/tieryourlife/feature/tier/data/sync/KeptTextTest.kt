package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupRefusal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeptTextTest {

    @Test
    fun `an attribution over 200 characters keeps its author licence and whole links`() {
        val credit = "© " + "Jane Doe ".repeat(30) + "· CC BY 4.0 · https://creativecommons.org/licenses/by/4.0/ · https://commons.wikimedia.org/?curid=12345"
        assertEquals(credit, KeptText.clean(credit, KeptText.CREDIT))
    }

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
    fun `a cut never leaves half of an emoji`() {
        val cut = KeptText.clean("a".repeat(KeptText.CAPTION - 1) + "😀", KeptText.CAPTION)

        assertEquals("a".repeat(KeptText.CAPTION - 1), cut)
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

    // P8: cut between the halves of an emoji, the account got a "?" mid-title.
    @Test
    fun `a title cut to make room for the suffix keeps every emoji whole`() {
        val suffix = "(from this phone)"
        val room = KeptText.TITLE - suffix.length - 1

        val renamed = withSuffix("a".repeat(room - 1) + "😀b", suffix)

        assertEquals("a".repeat(room - 1) + " " + suffix, renamed)
    }

    @Test
    fun `a suffix too long for the limit is cut whole too`() {
        val renamed = withSuffix("Films", "a".repeat(KeptText.TITLE - 2) + "😀")

        assertEquals(false, renamed.last().isHighSurrogate())
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
