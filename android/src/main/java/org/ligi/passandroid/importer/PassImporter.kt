package org.ligi.passandroid.importer

import androidx.annotation.StringRes
import org.ligi.passandroid.R
import org.ligi.passandroid.functions.readJSONSafely
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

/**
 * Imports pass files into [passesDir]: Passbook (.pkpass), esPass, collections of Passbook passes (.pkpasses)
 * and - via [fallbackImport] - files that are no pass archive (images, PDFs).
 *
 * Archives are checked before anything is stored: paths must stay inside the target, sizes and the number of
 * files are limited and pass ids must be safe directory names.
 *
 * @param workDir directory for temporary files (e.g. the cache directory)
 */
class PassImporter(
    private val passesDir: File,
    private val workDir: File,
    private val fallbackImport: FallbackImport,
) {

    /**
     * @param source where the pass came from (stored with the pass)
     * @param overwrite true when updating a pass: an existing pass with the same id is replaced instead of kept
     */
    fun import(input: InputStream, source: String, overwrite: Boolean = false): ImportResult {
        val tempFile = File.createTempFile("ins", "pass", workDir)
        return try {
            input.use { stream ->
                FileOutputStream(tempFile).use { output -> stream.copyToWithLimit(output, MAX_IMPORTED_STREAM_BYTES) }
            }
            importFile(tempFile, source, overwrite)
        } catch (e: ImportValidationException) {
            ImportResult.failed(ImportError(e.messageRes))
        } catch (e: Exception) {
            ImportResult.failed(ImportError(R.string.import_error_read_file, e.reason()))
        } finally {
            tempFile.delete()
        }
    }

    private fun importFile(file: File, source: String, overwrite: Boolean): ImportResult =
        if (isPkpassesBundle(file)) importBundle(file, source, overwrite) else importSinglePass(file, source, overwrite)

    private fun importSinglePass(file: File, source: String, overwrite: Boolean): ImportResult {
        val extractDir = File(workDir, "temp/${UUID.randomUUID()}")
        extractDir.mkdirs()
        if (!extractDir.exists()) return ImportResult.failed(ImportError(R.string.import_error_temp_dir))

        try {
            val isArchive = try {
                extractZipSafely(file, extractDir)
            } catch (e: ImportValidationException) {
                return ImportResult.failed(ImportError(e.messageRes))
            }

            val manifestFile = File(extractDir, "manifest.json")
            val espassFile = File(extractDir, "main.json")
            val passId = when {
                manifestFile.exists() -> readId(manifestFile, "pass.json")
                espassFile.exists() -> readId(espassFile, "id")
                !isArchive -> return fallbackImport.import(file)
                    ?.let { ImportResult(listOf(it), emptyList()) }
                    ?: ImportResult.failed(ImportError(R.string.import_error_unknown_format))
                else -> return ImportResult.failed(ImportError(R.string.import_error_no_manifest))
            }.getOrElse { return ImportResult.failed(ImportError(R.string.import_error_manifest, it.reason())) }

            if (!SAFE_PASS_ID.matches(passId)) return ImportResult.failed(ImportError(R.string.import_error_invalid_pass_id))

            File(extractDir, "source.obj").writeText(source)
            return storePass(extractDir, passId, overwrite)
        } finally {
            if (extractDir.exists()) extractDir.deleteRecursively()
        }
    }

    /** the pass id from manifest.json (Passbook) or main.json (esPass) */
    private fun readId(file: File, key: String): Result<String> = runCatching {
        readJSONSafely(file.readText())!!.getString(key)
    }

    private fun storePass(extractDir: File, passId: String, overwrite: Boolean): ImportResult {
        passesDir.mkdirs()
        val target = File(passesDir, passId)
        if (!isInsideDirectory(passesDir, target)) return ImportResult.failed(ImportError(R.string.import_error_unsafe_target))

        if (overwrite && target.exists()) {
            target.deleteRecursively()
        }

        if (target.exists()) {
            Timber.i("Pass with same ID exists - keeping it")
        } else if (!extractDir.renameTo(target)) {
            return ImportResult.failed(ImportError(R.string.import_error_store))
        }
        return ImportResult(listOf(passId), emptyList())
    }

    private fun isPkpassesBundle(file: File): Boolean = try {
        ZipFile(file).use { zip ->
            var count = 0
            var hasPkpass = false
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory) {
                    entry.safeRelativeName()
                    hasPkpass = true
                    count++
                    if (count > MAX_PKPASS_BUNDLE_ENTRIES) {
                        throw ImportValidationException(R.string.import_error_bundle_too_many, "pkpasses bundle contains too many passes")
                    }
                    if (!entry.name.endsWith(".pkpass", ignoreCase = true)) {
                        return false
                    }
                }
            }
            hasPkpass
        }
    } catch (_: ZipException) {
        false
    }

    private fun importBundle(file: File, source: String, overwrite: Boolean): ImportResult {
        val passIds = mutableListOf<String>()
        val errors = mutableListOf<ImportError>()
        try {
            ZipFile(file).use { zip ->
                val pkpassEntries = zip.safeEntries(MAX_PKPASS_BUNDLE_ENTRIES)
                    .filterNot { it.isDirectory }
                    .filter { it.name.endsWith(".pkpass", ignoreCase = true) }

                var copiedBytes = 0L
                pkpassEntries.forEach { entry ->
                    entry.safeRelativeName()
                    if (entry.size > MAX_ZIP_ENTRY_BYTES) {
                        throw ImportValidationException(R.string.import_error_bundle_oversized_pass, "pkpasses bundle contains an oversized pass")
                    }
                    val tempPass = File.createTempFile("bundle-", ".pkpass", workDir)
                    try {
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(tempPass).use { output ->
                                copiedBytes += input.copyToWithLimit(output, MAX_ZIP_ENTRY_BYTES)
                            }
                        }
                        if (copiedBytes > MAX_ZIP_TOTAL_BYTES) {
                            throw ImportValidationException(R.string.import_error_bundle_too_large, "pkpasses bundle is too large")
                        }
                        val result = importSinglePass(tempPass, source, overwrite)
                        passIds += result.passIds
                        errors += result.errors
                    } finally {
                        tempPass.delete()
                    }
                }
            }
        } catch (e: ImportValidationException) {
            // the passes imported so far are stored - report them together with the problem
            return ImportResult(passIds, errors + ImportError(e.messageRes))
        } catch (e: Exception) {
            return ImportResult(passIds, errors + ImportError(R.string.import_error_bundle, e.reason()))
        }

        if (passIds.isEmpty()) {
            errors += ImportError(R.string.import_error_bundle_empty)
        }
        return ImportResult(passIds, errors)
    }

    /** returns false when the file is no zip archive */
    private fun extractZipSafely(sourceFile: File, destination: File): Boolean = try {
        ZipFile(sourceFile).use { zip ->
            var totalBytes = 0L
            zip.safeEntries(MAX_ZIP_ENTRIES).forEach { entry ->
                val target = File(destination, entry.safeRelativeName())
                if (!isInsideDirectory(destination, target)) {
                    throw ImportValidationException(R.string.import_error_unsafe_path, "Archive contains an unsafe path")
                }

                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    if (entry.size > MAX_ZIP_ENTRY_BYTES) {
                        throw ImportValidationException(R.string.import_error_oversized_file, "Archive contains an oversized file")
                    }
                    target.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(target).use { output ->
                            totalBytes += input.copyToWithLimit(output, MAX_ZIP_ENTRY_BYTES)
                        }
                    }
                    if (totalBytes > MAX_ZIP_TOTAL_BYTES) {
                        throw ImportValidationException(R.string.import_error_archive_too_large, "Archive is too large")
                    }
                }
            }
        }
        true
    } catch (_: ZipException) {
        false
    }

    private fun ZipEntry.safeRelativeName(): String {
        val normalized = name.replace('\\', '/')
        val segments = normalized.split('/')
        if (normalized.isBlank() || normalized.startsWith('/') || segments.any { it == "." || it == ".." }) {
            throw ImportValidationException(R.string.import_error_unsafe_path, "Archive contains an unsafe path")
        }
        return normalized
    }

    private fun isInsideDirectory(baseDir: File, target: File): Boolean =
        target.canonicalFile.path.startsWith(baseDir.canonicalFile.path + File.separator)

    private fun ZipFile.safeEntries(maxEntries: Int): List<ZipEntry> {
        val result = mutableListOf<ZipEntry>()
        val entries = entries()
        while (entries.hasMoreElements()) {
            if (result.size >= maxEntries) {
                throw ImportValidationException(R.string.import_error_too_many_files, "Archive contains too many files")
            }
            result += entries.nextElement()
        }
        return result
    }

    private fun InputStream.copyToWithLimit(output: OutputStream, maxBytes: Long): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) return totalBytes
            totalBytes += read
            if (totalBytes > maxBytes) {
                throw ImportValidationException(R.string.import_error_file_too_large, "Imported file is too large")
            }
            output.write(buffer, 0, read)
        }
    }

    private fun Throwable.reason() = localizedMessage ?: javaClass.simpleName

    /** a file rejected by the import checks: [messageRes] is shown to the user, the English [message] is tracked */
    private class ImportValidationException(@StringRes val messageRes: Int, message: String) : Exception(message)

    private companion object {
        const val MAX_IMPORTED_STREAM_BYTES = 100L * 1024L * 1024L
        const val MAX_ZIP_TOTAL_BYTES = 100L * 1024L * 1024L
        const val MAX_ZIP_ENTRY_BYTES = 25L * 1024L * 1024L
        const val MAX_ZIP_ENTRIES = 512
        const val MAX_PKPASS_BUNDLE_ENTRIES = 100
        val SAFE_PASS_ID = Regex("[A-Za-z0-9](?:[A-Za-z0-9._-]{0,126}[A-Za-z0-9])?")
    }
}
