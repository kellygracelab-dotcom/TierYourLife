package com.artiuillab.tieryourlife.feature.tier.board.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import androidx.core.graphics.createBitmap
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Scale
import coil3.toBitmap
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import com.artiuillab.tieryourlife.feature.tier.domain.model.coverFrame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Where your boards' collages are kept: the app's own cache, which is never
 * backed up and never sent anywhere. A collage is drawn once per [BoardCollage.key]
 * and then read back; the one it replaces goes, and the least recently shown
 * go past [maxFiles] or [maxBytes].
 */
class BoardCollages(
    private val dir: File,
    private val maxFiles: Int = MAX_FILES,
    private val maxBytes: Long = MAX_BYTES,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val draw: suspend (BoardCollage) -> Bitmap?,
) {
    private val ready = ConcurrentHashMap<String, File>()
    private val drawing = Semaphore(DRAWN_AT_ONCE)

    /** A collage already found or drawn while the app is open: shown in the tile's first frame. */
    fun known(collage: BoardCollage): File? = ready[collage.fileName]

    /** The collage kept from before, if there is one. */
    suspend fun find(collage: BoardCollage): File? = withContext(io) {
        File(dir, collage.fileName).takeIf(File::isFile)?.also {
            // Read back is used: the trim keeps what was shown last.
            it.setLastModified(System.currentTimeMillis())
            ready[it.name] = it
        }
    }

    /** The collage kept from before, or drawn and kept now; null when any of its pictures would not come. */
    suspend fun make(collage: BoardCollage): File? = drawing.withPermit {
        find(collage) ?: run {
            val bitmap = attempt("draw") { draw(collage) } ?: return@run null
            withContext(io) { attempt("keep") { keep(collage, bitmap) } }
        }
    }

    /** A kept collage that would not open is dropped, so the next look draws it again. */
    suspend fun forget(collage: BoardCollage) {
        ready.remove(collage.fileName)
        withContext(io) { File(dir, collage.fileName).delete() }
    }

    private fun keep(collage: BoardCollage, bitmap: Bitmap): File? {
        dir.mkdirs()
        val part = File.createTempFile(PART_PREFIX, PART_SUFFIX, dir)
        part.outputStream().use { bitmap.compress(WEBP, QUALITY, it) }
        val file = File(dir, collage.fileName)
        if (!part.renameTo(file)) {
            part.delete()
            return null
        }
        ready[file.name] = file
        dir.listFiles { old -> old.name.startsWith(collage.slot + "-") && old.name != file.name }
            ?.forEach(::drop)
        trim(file)
        return file
    }

    private fun trim(newest: File) {
        val now = System.currentTimeMillis()
        dir.listFiles { part -> part.name.endsWith(PART_SUFFIX) && now - part.lastModified() > STALE_PART_MS }
            ?.forEach(File::delete)
        val kept = dir.listFiles { file -> file.name.endsWith(WEBP_SUFFIX) && file != newest }.orEmpty()
            .sortedByDescending(File::lastModified)
        var files = 1
        var bytes = newest.length()
        kept.forEach { file ->
            files++
            bytes += file.length()
            if (files > maxFiles || bytes > maxBytes) drop(file)
        }
    }

    private fun drop(file: File) {
        ready.remove(file.name)
        file.delete()
    }

    private suspend fun <T> attempt(what: String, block: suspend () -> T?): T? = runCatching { block() }
        .onFailure { failed ->
            if (failed is CancellationException) throw failed
            Timber.w(failed, "Could not %s a board collage", what)
        }
        .getOrNull()

    companion object {
        private const val DIR = "board-collages"
        private const val MAX_FILES = 300
        private const val MAX_BYTES = 24L * 1024 * 1024
        private const val DRAWN_AT_ONCE = 2
        private const val QUALITY = 85
        private const val PART_PREFIX = "collage"
        private const val PART_SUFFIX = ".part"
        private const val WEBP_SUFFIX = ".webp"
        private const val STALE_PART_MS = 60_000L

        @Suppress("DEPRECATION")
        private val WEBP: Bitmap.CompressFormat =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP

        @Volatile
        private var shared: BoardCollages? = null

        /** The app's one store, drawing with the app's image loader. */
        fun of(context: Context): BoardCollages = shared ?: synchronized(this) {
            shared ?: context.applicationContext.let { app ->
                BoardCollages(File(app.cacheDir, DIR)) { collage -> drawCollage(app, collage) }
            }.also { shared = it }
        }
    }
}

/** Each picture fetched at its cell's size and cut by its focus with the formula every platform uses; null if any did not come. */
internal suspend fun drawCollage(context: Context, collage: BoardCollage): Bitmap? {
    val cells = collageCells(collage.pictures.size, collage.width, collage.height)
    val loader = SingletonImageLoader.get(context)
    val pictures = coroutineScope {
        collage.pictures.mapIndexed { index, picture ->
            async {
                val cell = cells[index]
                val zoom = collage.focus[index]?.zoom ?: CoverFocus.MIN_ZOOM
                val request = ImageRequest.Builder(context)
                    .data(picture)
                    .size(ceil(cell.width * zoom).toInt(), ceil(cell.height * zoom).toInt())
                    .scale(Scale.FILL)
                    // A hardware bitmap cannot be drawn onto a software canvas.
                    .allowHardware(false)
                    .build()
                (loader.execute(request) as? SuccessResult)?.image?.toBitmap()
            }
        }.awaitAll()
    }
    val loaded = pictures.filterNotNull()
    if (loaded.size != cells.size) return null
    return glue(loaded, cells, collage)
}

/** The pictures drawn into their cells on a transparent picture of the collage's size. */
internal fun glue(pictures: List<Bitmap>, cells: List<CollageCell>, collage: BoardCollage): Bitmap {
    val glued = createBitmap(collage.width, collage.height)
    val canvas = Canvas(glued)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    pictures.forEachIndexed { index, picture ->
        val cell = cells[index]
        val frame = coverFrame(
            picture.width.toFloat(),
            picture.height.toFloat(),
            cell.width.toFloat() / cell.height,
            collage.focus[index],
        )
        val source = Rect(
            frame.left.roundToInt(),
            frame.top.roundToInt(),
            (frame.left + frame.width).roundToInt(),
            (frame.top + frame.height).roundToInt(),
        )
        canvas.drawBitmap(picture, source, Rect(cell.left, cell.top, cell.right, cell.bottom), paint)
    }
    return glued
}
