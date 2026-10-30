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
import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import java.io.ByteArrayOutputStream
import java.io.InputStream

internal const val FACE_EDGE_PX = 512

private const val JPEG_QUALITY = 85
private const val QUARTER_TURN = 90f
private const val HALF_TURN = 180f
private const val THREE_QUARTER_TURN = 270f

/**
 * The photo as a [FACE_EDGE_PX] square JPEG: the square the person chose, or
 * the middle one when they chose none. Null when the picture cannot be decoded.
 */
internal fun squareFaceJpeg(resolver: ContentResolver, uri: Uri, crop: FaceCrop?): ByteArray? {
    val decoded = decodeShrunk(resolver, uri, crop) ?: return null
    val square = faceSquare(decoded.width, decoded.height, crop)
    val cut = Bitmap.createBitmap(decoded, square.left, square.top, square.side, square.side)
    val face = if (square.side == FACE_EDGE_PX) cut else cut.scale(FACE_EDGE_PX, FACE_EDGE_PX)
    val out = ByteArrayOutputStream()
    val written = face.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
    return out.toByteArray().takeIf { written }
}

private fun decodeShrunk(resolver: ContentResolver, uri: Uri, crop: FaceCrop?): Bitmap? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // Turns the picture the way the camera held it; the older decoder below does not.
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            decoder.setTargetSampleSize(faceSampleSize(info.size.width, info.size.height, crop))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        decodeShrunkTheOldWay(crop) { resolver.openInputStream(uri) }
    }

/** Measuring only, decodeStream always answers null; what counts is whether the file opened and had a size. */
private fun boundsOf(open: () -> InputStream?): BitmapFactory.Options? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
    return bounds.takeIf { it.outWidth > 0 && it.outHeight > 0 }
}

private fun decodeShrunkTheOldWay(crop: FaceCrop?, open: () -> InputStream?): Bitmap? {
    val bounds = boundsOf(open) ?: return null

    val orientation = open()?.use { stream ->
        ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL
    // The crop was chosen on the upright picture; a quarter turn swaps which edge is which.
    val turned = orientation in QUARTER_TURNS
    val width = if (turned) bounds.outHeight else bounds.outWidth
    val height = if (turned) bounds.outWidth else bounds.outHeight
    val options = BitmapFactory.Options().apply { inSampleSize = faceSampleSize(width, height, crop) }
    val bitmap = open()?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    return turnUpright(bitmap, orientation)
}

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
