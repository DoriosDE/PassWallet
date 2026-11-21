package org.ligi.passandroid.share

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import java.io.File

/**
 * Packs the files in [passDirectory] into [target] (an esPass file: a zip without root folder).
 * An existing [target] is replaced; when packing fails, no half written [target] is left and the exception is thrown.
 */
fun exportPass(passDirectory: File, target: File) {
    try {
        target.delete()
        target.parentFile?.mkdirs()
        val parameters = ZipParameters().apply {
            isIncludeRootFolder = false
            compressionMethod = CompressionMethod.DEFLATE
            compressionLevel = CompressionLevel.NORMAL
        }
        ZipFile(target).use { it.addFolder(passDirectory, parameters) }
    } catch (e: Exception) {
        target.delete()
        throw e
    }
}
