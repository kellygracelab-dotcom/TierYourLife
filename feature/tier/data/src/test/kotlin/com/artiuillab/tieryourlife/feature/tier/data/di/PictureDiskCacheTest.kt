package com.artiuillab.tieryourlife.feature.tier.data.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PictureDiskCacheTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun pictures_areKeptInANewFolder_andTheOneWithReplacedOriginalsGoes() {
        val cacheDir = folder.root
        val replaced = cacheDir.resolve(REPLACED_PICTURE_CACHE).resolve("journal").apply {
            parentFile?.mkdirs()
            writeText("old")
        }
        val kept = cacheDir.resolve("http_cache").apply { mkdirs() }

        val cache = pictureDiskCache(cacheDir)

        assertFalse(replaced.parentFile!!.exists())
        assertEquals(cacheDir.resolve(PICTURE_CACHE).absolutePath, cache.directory.toFile().absolutePath)
        assertTrue(kept.exists())
    }
}
