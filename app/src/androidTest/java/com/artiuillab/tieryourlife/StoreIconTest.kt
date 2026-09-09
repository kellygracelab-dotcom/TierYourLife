package com.artiuillab.tieryourlife

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The 512x512 icon Google Play asks for, drawn from the same adaptive icon the
 * launcher uses instead of being exported by hand. The largest one shipped in
 * the app is 192px and the artwork is vector, so there is no reason for the
 * store icon to be a separate file that quietly drifts from the real one.
 *
 * Nothing asserts. `docs/screenshots.md` says how to run it and where the file
 * goes.
 */
@RunWith(AndroidJUnit4::class)
class StoreIconTest {

    @Test
    fun theIconGooglePlayAsksFor() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val icon = context.getDrawable(R.mipmap.ic_launcher)!!

        // An adaptive icon is drawn on a 108dp canvas of which only the middle
        // 72dp is ever shown, and drawing it applies the device's mask -- so the
        // whole canvas comes back with cut corners, not with the outer stripes.
        // Draw it oversized and keep the part that survives the mask: what the
        // launcher shows, at the size Play wants, opaque to the edges.
        val canvasSize = SIZE * CANVAS / VISIBLE
        val whole = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, canvasSize, canvasSize)
        icon.draw(Canvas(whole))

        val inset = (canvasSize - SIZE) / 2
        val visible = Bitmap.createBitmap(whole, inset, inset, SIZE, SIZE)

        val outputDir = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?: context.filesDir.absolutePath
        val dir = File(outputDir, "store").apply { mkdirs() }
        File(dir, "icon-512.png").outputStream().use { visible.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val SIZE = 512
        const val CANVAS = 108
        const val VISIBLE = 72
    }
}
