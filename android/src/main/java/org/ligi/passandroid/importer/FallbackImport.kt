package org.ligi.passandroid.importer

import java.io.File

/**
 * Turns a file that is no pass archive (e.g. an image or a PDF) into a pass.
 * Returns the id of the new pass, or null when the file cannot be used.
 */
fun interface FallbackImport {
    fun import(file: File): String?
}
