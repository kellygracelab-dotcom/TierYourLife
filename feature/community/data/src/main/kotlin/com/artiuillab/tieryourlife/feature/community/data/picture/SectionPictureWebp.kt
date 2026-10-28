package com.artiuillab.tieryourlife.feature.community.data.picture

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.artiuillab.tieryourlife.feature.community.domain.model.PictureCrop
import com.artiuillab.tieryourlife.feature.community.domain.repository.PictureSize
import java.io.ByteArrayOutputStream
import java.io.InputStream

private const val WEBP_QUALITY = 82
private const val QUARTER_TURN = 90f
private const val HALF_TURN = 180f
private const val THREE_QUARTER_TURN = 270f

// Written after the face's own decoding (account's FacePhotoJpeg), which the
// module rules keep out of reach: the same upright reading, a 16:10 cut
// instead of a square.

/** The picture's size as it stands upright; null when it cannot be read. */
internal fun uprightSize(resolver: ContentResolver, uri: Uri): PictureSize? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val turned = orientationOf { resolver.openInputStream(uri) } in QUARTER_TURNS
    return if (turned) PictureSize(bounds.outHeight, bounds.outWidth) else PictureSize(bounds.outWidth, bounds.outHeight)
}

/** The cut as a WebP, 1440 × 900 at most and 960 × 600 at least; null when it cannot be decoded or is too small. */
internal fun sectionPictureWebp(resolver: ContentResolver, uri: Uri, crop: PictureCrop): ByteArray? {
    val size = uprightSize(resolver, uri) ?: return null
    val sample = sectionSampleSize(size.width, crop)
    val decoded = decode(resolver, uri, sample) ?: return null
    val cut = sectionCut(decoded.width, decoded.height, crop, scale = size.width / decoded.width.coerceAtLeast(1)) ?: return null
    val piece = Bitmap.createBitmap(decoded, cut.left, cut.top, cut.width, cut.height)
    val exported = if (piece.width == cut.exportWidth) piece else piece.scale(cut.exportWidth, cut.exportHeight)
    val out = ByteArrayOutputStream()
    val written = exported.compress(webp(), WEBP_QUALITY, out)
    return out.toByteArray().takeIf { written }
}

@Suppress("DEPRECATION")
private fun webp(): Bitmap.CompressFormat =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP

private fun decode(resolver: ContentResolver, uri: Uri, sample: Int): Bitmap? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // Turns the picture the way the camera held it; the older decoder below does not.
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, _, _ ->
            decoder.setTargetSampleSize(sample)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?.let { turnUpright(it, orientationOf { resolver.openInputStream(uri) }) }
    }

private fun orientationOf(open: () -> InputStream?): Int =
    open()?.use { stream -> ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        ?: ExifInterface.ORIENTATION_NORMAL

private val QUARTER_TURNS = setOf(
    ExifInterface.ORIENTATION_ROTATE_90,
    ExifInterface.ORIENTATION_ROTATE_270,
    ExifInterface.ORIENTATION_TRANSPOSE,
    ExifInterface.ORIENTATION_TRANSVERSE,
)

private fun turnUpright(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(QUARTER_TURN)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(HALF_TURN)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(THREE_QUARTER_TURN)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(QUARTER_TURN)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(THREE_QUARTER_TURN)
            matrix.postScale(-1f, 1f)
        }
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
