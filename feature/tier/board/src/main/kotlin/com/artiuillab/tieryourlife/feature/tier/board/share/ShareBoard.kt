package com.artiuillab.tieryourlife.feature.tier.board.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Scale
import coil3.toBitmap
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.File

/** Where the picture is sent from. Matches the provider declared by the app. */
private const val SHARED_DIR = "shared"
private const val AUTHORITY_SUFFIX = ".share"

/** Covers are decoded at twice a large cell's width; a bigger one already in memory serves too. */
private const val COVER_WIDTH = 288
private const val COVER_HEIGHT = 412

/** Covers fetched at once, and how long the picture waits for them all: one slow cover never holds it back. */
const val COVERS_AT_ONCE = 6
const val COVERS_WAIT_MS = 4_000L

/** Where the caption points. Not Play itself: until launch its page is "not found" to anyone who is not a tester, and this page links to it. */
const val SHARE_LINK = "https://tieryourlife.com"

/**
 * Fetches the covers [items] needs, [COVERS_AT_ONCE] at a time, reporting
 * each as it comes so the sheet can draw it in, and gives up on the rest
 * after [COVERS_WAIT_MS]: a card whose cover has not come is drawn as its title.
 */
suspend fun loadCovers(context: Context, items: List<TierItem>, onCover: (Long, Bitmap) -> Unit) {
    val loader = SingletonImageLoader.get(context)
    val gate = Semaphore(COVERS_AT_ONCE)
    withTimeoutOrNull(COVERS_WAIT_MS) {
        coroutineScope {
            items.filter { it.imageUrl != null }.distinctBy { it.id }.forEach { item ->
                launch {
                    val bitmap = gate.withPermit {
                        val request = ImageRequest.Builder(context)
                            .data(item.imageUrl)
                            .size(COVER_WIDTH, COVER_HEIGHT)
                            .scale(Scale.FILL)
                            // A hardware bitmap cannot be drawn onto a software canvas.
                            .allowHardware(false)
                            .build()
                        (loader.execute(request) as? SuccessResult)?.image?.toBitmap()
                    }
                    if (bitmap != null) onCover(item.id, bitmap)
                }
            }
        }
    }
}

/** The picture written where the share sheet can read it; null when it could not be. One file, overwritten: a share is a moment, not a library. */
suspend fun writePicture(context: Context, bitmap: Bitmap): Uri? {
    val file = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, SHARED_DIR).apply { mkdirs() }
            File(dir, "board.png").also { file -> file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }.onFailure { Timber.w(it, "Could not write the board picture") }.getOrNull()
    } ?: return null
    return runCatching { FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file) }
        .onFailure { Timber.w(it, "No provider for the shared picture") }
        .getOrNull()
}

/**
 * The chooser with the picture and its [caption]; the caption matters as much
 * as the picture, since a chat shows it under the photo and it is the only
 * part a friend can tap. False when nothing could take it.
 */
fun sendPicture(context: Context, uri: Uri, caption: String): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching { context.startActivity(Intent.createChooser(send, null)) }
        .onFailure { Timber.w(it, "Nothing to share the picture with") }
        .isSuccess
}

/**
 * The last picture made, kept until the board changes: sharing the same
 * board again is instant. Keyed by everything drawn, the board included.
 */
internal object SharedPictures {
    private var last: Pair<Any, Uri>? = null

    fun get(key: Any): Uri? = last?.takeIf { it.first == key }?.second

    fun keep(key: Any, uri: Uri) {
        last = key to uri
    }

    fun forget() {
        last = null
    }
}

/** Everything the picture is drawn from: a change to any of it is a new picture. */
internal data class PictureKey(val list: TierList, val names: Boolean, val byLine: String, val footer: String)
