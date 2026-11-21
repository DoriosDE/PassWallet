package org.ligi.passandroid.ui.scan

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap

/** a barcode found by the scanner - [format] is the name of the ZXing [BarcodeFormat] */
data class ScannedBarcode(val format: String, val text: String)

/**
 * Finds barcodes of all formats ZXing knows in camera frames and pictures, also light barcodes on dark ground.
 * Can be used from several threads: decoding runs one at a time.
 */
class BarcodeDecoder {

    private val reader = MultiFormatReader().apply {
        setHints(
            EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.TRY_HARDER, true)
                put(DecodeHintType.POSSIBLE_FORMATS, BarcodeFormat.entries)
            },
        )
    }

    /** the barcode in a camera frame, or null */
    fun decode(image: ImageProxy): ScannedBarcode? = decode(image.toLuminanceSource())

    /** the barcode in [bitmap], also when the picture is turned by 90, 180 or 270 degrees, or null */
    fun decode(bitmap: Bitmap): ScannedBarcode? {
        return listOf(0f, 90f, 180f, 270f).firstNotNullOfOrNull { degrees ->
            val candidate = if (degrees == 0f) bitmap else rotateBitmap(bitmap, degrees)
            try {
                candidate?.let { decodePixels(it.width, it.height, it.pixels()) }
            } finally {
                if (candidate != null && candidate !== bitmap) {
                    candidate.recycle()
                }
            }
        }
    }

    /** the barcode in the ARGB [pixels] of a picture of [width] x [height], row by row, or null */
    fun decodePixels(width: Int, height: Int, pixels: IntArray): ScannedBarcode? =
        decode(RGBLuminanceSource(width, height, pixels))

    private fun decode(source: LuminanceSource): ScannedBarcode? =
        listOf(source, source.invert()).firstNotNullOfOrNull { decode(BinaryBitmap(HybridBinarizer(it))) }

    private fun decode(candidate: BinaryBitmap): ScannedBarcode? {
        return synchronized(reader) {
            try {
                reader.decodeWithState(candidate)
                    .takeIf { it.text.isNotBlank() }
                    ?.let { ScannedBarcode(it.barcodeFormat.name, it.text) }
            } catch (_: NotFoundException) {
                null
            } finally {
                reader.reset()
            }
        }
    }
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap? {
    return runCatching {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
    }.getOrNull()
}

private fun Bitmap.pixels(): IntArray {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    return pixels
}

/** the brightness (Y plane) of a camera frame */
private fun ImageProxy.toLuminanceSource(): PlanarYUVLuminanceSource {
    val plane = planes.first()
    val width = width
    val height = height
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val data = ByteArray(width * height)
    var outputOffset = 0
    for (row in 0 until height) {
        buffer.position(row * rowStride)
        buffer.get(data, outputOffset, width)
        outputOffset += width
    }
    return PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
}
