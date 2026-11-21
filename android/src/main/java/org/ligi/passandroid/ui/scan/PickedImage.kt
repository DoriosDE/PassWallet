package org.ligi.passandroid.ui.scan

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

private const val MAX_PICKED_IMAGE_SIZE = 1800

/**
 * Reads the picture at [uri], scaled down by powers of two until neither side is longer than
 * [MAX_PICKED_IMAGE_SIZE] - big enough to find a barcode, small enough for memory. Null when it cannot be read.
 */
fun ContentResolver.loadPickedImage(uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

    var sampleSize = 1
    while ((bounds.outWidth / sampleSize) > MAX_PICKED_IMAGE_SIZE || (bounds.outHeight / sampleSize) > MAX_PICKED_IMAGE_SIZE) {
        sampleSize *= 2
    }

    return openInputStream(uri)?.use {
        BitmapFactory.decodeStream(
            it,
            null,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        )
    }
}
