package org.ligi.passandroid.model.pass

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toDrawable
import com.squareup.moshi.JsonClass
import timber.log.Timber
import java.util.Locale

@JsonClass(generateAdapter = true)
class BarCode(val format: PassBarCodeFormat?, val message: String? = "") {

    var alternativeText: String? = null

    @Transient
    private var cachedBitmap: Bitmap? = null

    @Transient
    private var cachedBitmapWidthPx: Int? = null

    @Transient
    private var cachedBitmapHeightPx: Int? = null

    fun getBitmap(resources: Resources, widthPx: Int? = null, heightPx: Int? = null): BitmapDrawable? {
        if (message == null) {
            // no message -> no barcode
            return null
        }

        val barcodeFormat = if (format == null) {
            Timber.w("Barcode format is null - fallback to QR")
            PassBarCodeFormat.QR_CODE
        } else {
            format
        }

        val bitmap = cachedBitmap
            ?.takeIf { cachedBitmapWidthPx == widthPx && cachedBitmapHeightPx == heightPx }
            ?: generateBarCodeBitmap(message, barcodeFormat, widthPx, heightPx)?.also {
                cachedBitmap = it
                cachedBitmapWidthPx = widthPx
                cachedBitmapHeightPx = heightPx
            } ?: return null

        return bitmap.toDrawable(resources).apply {
            isFilterBitmap = false
            setAntiAlias(false)
        }

    }

    companion object {

        fun getFormatFromString(format: String): PassBarCodeFormat {
            return when {
                format.uppercase(Locale.ENGLISH).contains("AZTEC") -> PassBarCodeFormat.AZTEC
                format.uppercase(Locale.ENGLISH).contains("CODABAR") -> PassBarCodeFormat.CODABAR
                format.uppercase(Locale.ENGLISH).contains("93") -> PassBarCodeFormat.CODE_93
                format.uppercase(Locale.ENGLISH).contains("128") -> PassBarCodeFormat.CODE_128
                format.uppercase(Locale.ENGLISH).contains("39") -> PassBarCodeFormat.CODE_39
                format.uppercase(Locale.ENGLISH).contains("DATA_MATRIX") -> PassBarCodeFormat.DATA_MATRIX
                format.uppercase(Locale.ENGLISH).contains("EAN_8") -> PassBarCodeFormat.EAN_8
                format.uppercase(Locale.ENGLISH).contains("EAN_13") -> PassBarCodeFormat.EAN_13
                format.uppercase(Locale.ENGLISH).contains("ITF") -> PassBarCodeFormat.ITF
                format.contains("417") -> PassBarCodeFormat.PDF_417
                format.uppercase(Locale.ENGLISH).contains("UPC_A") -> PassBarCodeFormat.UPC_A
                format.uppercase(Locale.ENGLISH).contains("UPC_E") -> PassBarCodeFormat.UPC_E
                else -> PassBarCodeFormat.QR_CODE

            }


        }
    }

}
