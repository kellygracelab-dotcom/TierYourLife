package com.artiuillab.tieryourlife.feature.tier.data.local.image

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * What a picture is called, and whether this phone has it.
 *
 * The whole of sync's picture half rests on one claim: the file's name means
 * the same thing on a second phone and the path around it does not.
 */
class TierImageStoreIdentityTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun store(directory: File = folder.root) =
        TierImageStore(directory = directory, openSource = { null })

    @Test
    fun `a picture is named by its file, not by where the file sits`() {
        val store = store()
        val path = store.write("pic-1", byteArrayOf(1, 2, 3))

        assertEquals("pic-1", store.pictureIdOf(path))
    }

    // The same picture on two phones. Same name, different directory, and only
    // the name is allowed to matter.
    @Test
    fun `the same name under a different directory is the same picture`() {
        val here = store(folder.newFolder("phone-one"))
        val there = store(folder.newFolder("phone-two"))

        assertEquals(
            here.pictureIdOf(here.write("pic-1", byteArrayOf(1))),
            there.pictureIdOf(there.write("pic-1", byteArrayOf(2))),
        )
    }

    // A poster is on somebody else's server and stays there; it is not ours to
    // keep and has no id of ours.
    @Test
    fun `a picture from a catalogue has no id of ours`() {
        assertNull(store().pictureIdOf("https://image.tmdb.org/t/p/w500/a.jpg"))
        assertNull(store().pictureIdOf(null))
    }

    // A path from somewhere else on the disk is not one of ours either, and
    // treating it as one would send an id nothing here can answer for.
    @Test
    fun `a file outside our own directory has no id`() {
        val outside = File(folder.newFolder("downloads"), "elsewhere.jpg")

        assertNull(store().pictureIdOf(outside.absolutePath))
    }

    @Test
    fun `a picture that has not arrived is not held`() {
        val store = store()

        assertFalse(store.holds("pic-1"))
        assertNull(store.read("pic-1"))
        // The path exists as an answer even when the file does not: it is
        // where the picture will be written when it comes down.
        assertTrue(store.pathFor("pic-1").endsWith("pic-1"))
    }

    @Test
    fun `a picture that arrived can be read straight back`() {
        val store = store()
        store.write("pic-1", byteArrayOf(7, 8, 9))

        assertTrue(store.holds("pic-1"))
        assertArrayEquals(byteArrayOf(7, 8, 9), store.read("pic-1"))
    }

    // Written beside the target and moved into place. Half a file is
    // indistinguishable from a whole one, and would be shown as a broken
    // picture forever rather than fetched again.
    @Test
    fun `an empty file does not count as a picture`() {
        val store = store()
        File(folder.root, "pic-1").createNewFile()

        assertFalse(store.holds("pic-1"))
    }
}
