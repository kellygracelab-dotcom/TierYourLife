package com.artiuillab.tieryourlife.feature.tier.presentation.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/** Where the picture is sent from. Matches the provider declared by the app. */
private const val SHARED_DIR = "shared"
private const val AUTHORITY_SUFFIX = ".share"

/** A poster that has not arrived in this long is drawn as its title instead. */
private const val PICTURE_WIDTH = 264

/**
 * Where the caption points. The store listing replaces this the day it exists;
 * until then the site, which is where the store link will lead anyway.
 */
const val SHARE_LINK = "https://tieryourlife.web.app"

/**
 * Sends a board as a picture, with a line of text beside it.
 *
 * The text matters as much as the picture: a chat app shows it as the caption,
 * and it is the only part of the message a friend can tap. The picture says
 * what the board is; the caption says where it was made and how to get there.
 *
 * Returns false when the picture could not be made or handed over, so the
 * screen can say so; nothing here throws at a person.
 */
suspend fun shareBoard(context: Context, list: TierList, palette: BoardPalette, caption: String, footer: String): Boolean {
    val pictures = loadPictures(context, list)
    val bitmap = BoardPicture.render(list, palette, pictures, footer)
    val file = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, SHARED_DIR).apply { mkdirs() }
            // One file, overwritten: a share is a moment, not a library, and
            // the cache is not a place to leave a picture per press.
            File(dir, "board.png").also { file ->
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }.onFailure { Timber.w(it, "Could not write the board picture") }.getOrNull()
    } ?: return false

    val uri = runCatching { FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file) }
        .onFailure { Timber.w(it, "No provider for the shared picture") }
        .getOrNull() ?: return false

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
 * Every card's picture, fetched together. A card whose picture does not come
 * is simply absent from the map and drawn as its title -- a friend gets the
 * board a little plainer rather than not at all.
 */
private suspend fun loadPictures(context: Context, list: TierList): Map<Long, Bitmap> = coroutineScope {
    val loader = SingletonImageLoader.get(context)
    list.tiers.flatMap { it.items }
        .filter { it.imageUrl != null }
        .map { item ->
            async {
                val request = ImageRequest.Builder(context)
                    .data(item.imageUrl)
                    .size(PICTURE_WIDTH)
                    // A hardware bitmap cannot be drawn onto a software canvas.
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request) as? SuccessResult
                result?.image?.toBitmap()?.let { item.id to it }
            }
        }
        .mapNotNull { it.await() }
        .toMap()
}
