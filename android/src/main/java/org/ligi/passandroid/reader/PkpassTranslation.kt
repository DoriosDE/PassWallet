package org.ligi.passandroid.reader

import androidx.annotation.VisibleForTesting
import com.ibm.icu.text.CharsetDetector
import timber.log.Timber
import java.io.File
import java.nio.charset.Charset

/**
 * Translations of a Passbook pass (pass.strings of the matching language): key -> translated text.
 */
class PkpassTranslation : HashMap<String, String>() {

    /** the translation of [key], or [key] itself when there is none */
    fun translate(key: String?): String? = key?.let { get(it) } ?: key

    fun loadFromFile(file: File) {
        loadFromString(readFileAsStringGuessEncoding(file))
    }

    @VisibleForTesting
    fun loadFromString(inputString: String?) {
        inputString ?: return

        for (pair in inputString.split("\";")) {
            val keyAndValue = pair.split(Regex("\" ?= ?\""))
            if (keyAndValue.size == 2) {
                put(keyAndValue[0].trimStart('"', '\n', '\r', ' '), keyAndValue[1])
            }
        }
    }

    companion object {
        private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

        /**
         * Reads a text file whose encoding is not known (Passbook files come as UTF-8, UTF-16 or worse).
         * Returns null when the file cannot be read.
         */
        fun readFileAsStringGuessEncoding(file: File): String? = try {
            val fileData = file.readBytes()
            if (fileData.size >= UTF8_BOM.size && fileData.copyOfRange(0, UTF8_BOM.size).contentEquals(UTF8_BOM)) {
                String(fileData, UTF8_BOM.size, fileData.size - UTF8_BOM.size, Charsets.UTF_8)
            } else {
                val detectedCharset = CharsetDetector().setText(fileData).detect()?.name
                    ?.takeIf { runCatching { Charset.isSupported(it) }.getOrDefault(false) }
                if (detectedCharset != null) String(fileData, charset(detectedCharset)) else String(fileData)
            }
        } catch (e: Exception) {
            Timber.w(e, "could not read pass translation file")
            null
        }
    }
}
