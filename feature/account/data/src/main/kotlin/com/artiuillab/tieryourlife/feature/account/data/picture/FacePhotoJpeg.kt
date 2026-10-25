package com.artiuillab.tieryourlife.feature.account.data.picture

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream

internal const val FACE_EDGE_PX = 512

private const val JPEG_QUALITY = 85
private const val QUARTER_TURN = 90f
private const val HALF_TURN = 180f
private const val THREE_QUARTER_TURN = 270f

/** How far to shrink while decoding so the shorter edge still covers the face. */
internal fun faceSampleSize(width: Int, height: Int): Int {
    var sample = 1
    while (minOf(width, height) / (sample * 2) >= FACE_EDGE_PX) {
        sample *= 2
    }
    return sample
}

/**
 * The photo as a [FACE_EDGE_PX] square JPEG, taken from the middle when it was
 * not square. Null when the picture cannot be decoded.
 */
internal fun squareFaceJpeg(resolver: ContentResolver, uri: Uri): ByteArray? {
    val decoded = decodeShrunk(resolver, uri) ?: return null
    val side = minOf(decoded.width, decoded.height)
    val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
    val face = if (side == FACE_EDGE_PX) square else square.scale(FACE_EDGE_PX, FACE_EDGE_PX)
    val out = ByteArrayOutputStream()
    val written = face.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
    return out.toByteArray().takeIf { written }
}

private fun decodeShrunk(resolver: ContentResolver, uri: Uri): Bitmap? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // Turns the picture the way the camera held it; the older decoder below does not.
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            decoder.setTargetSampleSize(faceSampleSize(info.size.width, info.size.height))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        decodeShrunkTheOldWay { resolver.openInputStream(uri) }
    }

private fun decodeShrunkTheOldWay(open: () -> InputStream?): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open()?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply { inSampleSize = faceSampleSize(bounds.outWidth, bounds.outHeight) }
    val bitmap = open()?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    val orientation = open()?.use { stream ->
        ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL
    return turnUpright(bitmap, orientation)
}

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
