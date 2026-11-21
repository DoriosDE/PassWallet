package org.ligi.passandroid.unittest

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.ui.scan.BarcodeDecoder
import org.ligi.passandroid.ui.scan.ScannedBarcode

class TheBarcodeDecoder {

    private val tested = BarcodeDecoder()

    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    /** ARGB pixels of the barcode, dark on light - or light on dark when [inverted] */
    private fun pixelsOf(text: String, format: BarcodeFormat, width: Int, height: Int, inverted: Boolean = false): IntArray {
        val matrix = MultiFormatWriter().encode(text, format, width, height)
        return IntArray(width * height) { index ->
            val set = matrix.get(index % width, index / width)
            if (set != inverted) black else white
        }
    }

    @Test
    fun findsAQrCode() {
        val pixels = pixelsOf("https://example.org/pass", BarcodeFormat.QR_CODE, 300, 300)

        assertThat(tested.decodePixels(300, 300, pixels))
            .isEqualTo(ScannedBarcode("QR_CODE", "https://example.org/pass"))
    }

    @Test
    fun findsALightQrCodeOnDarkGround() {
        val pixels = pixelsOf("light on dark", BarcodeFormat.QR_CODE, 300, 300, inverted = true)

        assertThat(tested.decodePixels(300, 300, pixels))
            .isEqualTo(ScannedBarcode("QR_CODE", "light on dark"))
    }

    @Test
    fun findsAOneDimensionalBarcode() {
        val pixels = pixelsOf("PASS-1234", BarcodeFormat.CODE_128, 600, 150)

        assertThat(tested.decodePixels(600, 150, pixels))
            .isEqualTo(ScannedBarcode("CODE_128", "PASS-1234"))
    }

    @Test
    fun findsNothingInAnEmptyPicture() {
        assertThat(tested.decodePixels(200, 200, IntArray(200 * 200) { white })).isNull()
    }

    @Test
    fun canBeUsedAgainAfterAFailedSearch() {
        tested.decodePixels(200, 200, IntArray(200 * 200) { white })

        val pixels = pixelsOf("again", BarcodeFormat.AZTEC, 300, 300)

        assertThat(tested.decodePixels(300, 300, pixels)).isEqualTo(ScannedBarcode("AZTEC", "again"))
    }
}
