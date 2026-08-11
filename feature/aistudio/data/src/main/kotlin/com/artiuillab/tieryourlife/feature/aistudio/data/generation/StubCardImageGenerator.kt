package com.artiuillab.tieryourlife.feature.aistudio.data.generation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.ConnectivityManager
import android.net.Uri
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.CardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val GENERATION_DELAY_MILLIS = 1200L
private const val IMAGE_WIDTH = 768
private const val IMAGE_HEIGHT = 1024
private const val CACHE_SUBDIRECTORY = "aistudio"
private const val PALETTE_COUNT = 3

@Singleton
class StubCardImageGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
) : CardImageGenerator {

    private val callCounts = ConcurrentHashMap<String, Int>()

    override suspend fun generate(prompt: String): GeneratedCardImage = withContext(Dispatchers.IO) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager?.activeNetwork == null) {
            throw IOException("No active network")
        }

        delay(GENERATION_DELAY_MILLIS)

        val callIndex = callCounts.merge(prompt, 1, Int::plus) ?: 1
        val paletteIndex = Math.floorMod(prompt.hashCode() + callIndex, PALETTE_COUNT)
        val bitmap = renderComposition(paletteIndex)

        val directory = File(context.cacheDir, CACHE_SUBDIRECTORY).apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}.png")
        FileOutputStream(file).use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
        bitmap.recycle()

        GeneratedCardImage(prompt = prompt, imageUri = Uri.fromFile(file).toString())
    }

    override suspend fun discard(image: GeneratedCardImage): Unit = withContext(Dispatchers.IO) {
        val path = Uri.parse(image.imageUri).path ?: return@withContext
        val file = File(path)
        val directory = File(context.cacheDir, CACHE_SUBDIRECTORY)
        if (file.absoluteFile.parentFile == directory.absoluteFile) {
            file.delete()
        }
    }
}

private fun renderComposition(paletteIndex: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val palette = PALETTES[paletteIndex]

    val gradientPaint = Paint().apply {
        shader = LinearGradient(
            0f,
            0f,
            0f,
            IMAGE_HEIGHT.toFloat(),
            palette.top,
            palette.bottom,
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), IMAGE_HEIGHT.toFloat(), gradientPaint)

    val circlePaint = Paint().apply { isAntiAlias = true }
    palette.circles.forEach { circle ->
        circlePaint.color = circle.color
        circlePaint.alpha = circle.alpha
        canvas.drawCircle(
            circle.centerXFraction * IMAGE_WIDTH,
            circle.centerYFraction * IMAGE_HEIGHT,
            circle.radiusFraction * IMAGE_WIDTH,
            circlePaint,
        )
    }

    return bitmap
}

private data class Circle(
    val centerXFraction: Float,
    val centerYFraction: Float,
    val radiusFraction: Float,
    val color: Int,
    val alpha: Int,
)

private data class Palette(val top: Int, val bottom: Int, val circles: List<Circle>)

private val PALETTES = listOf(
    Palette(
        top = 0xFF3A2E63.toInt(),
        bottom = 0xFFEADDFF.toInt(),
        circles = listOf(
            Circle(0.25f, 0.2f, 0.35f, 0xFFD0BCFF.toInt(), 110),
            Circle(0.75f, 0.4f, 0.28f, 0xFF6750A4.toInt(), 90),
            Circle(0.4f, 0.75f, 0.4f, 0xFFEADDFF.toInt(), 130),
        ),
    ),
    Palette(
        top = 0xFF00363D.toInt(),
        bottom = 0xFFB8EAFF.toInt(),
        circles = listOf(
            Circle(0.7f, 0.15f, 0.3f, 0xFF4FD8EB.toInt(), 100),
            Circle(0.2f, 0.5f, 0.32f, 0xFF006874.toInt(), 90),
            Circle(0.6f, 0.8f, 0.36f, 0xFFB8EAFF.toInt(), 120),
            Circle(0.15f, 0.85f, 0.2f, 0xFF4FD8EB.toInt(), 80),
        ),
    ),
    Palette(
        top = 0xFF680003.toInt(),
        bottom = 0xFFFFDAD4.toInt(),
        circles = listOf(
            Circle(0.3f, 0.25f, 0.33f, 0xFFFF897A.toInt(), 110),
            Circle(0.8f, 0.55f, 0.3f, 0xFFBA1B1B.toInt(), 95),
            Circle(0.5f, 0.85f, 0.38f, 0xFFFFDAD4.toInt(), 125),
        ),
    ),
)
