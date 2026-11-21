package org.ligi.passandroid.backup

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.ligi.passandroid.model.FileSystemPassStore.Companion.ARCHIVE_STATE_FILE
import java.io.IOException
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupArchive {

    private const val BACKUP_APP = "PassWallet"
    private const val MANIFEST_ENTRY = "manifest.json"
    private const val PASSES_PREFIX = "passes/"
    private const val PREFERENCES_ENTRY = "preferences.json"
    private const val MAX_BACKUP_TOTAL_BYTES = 100L * 1024L * 1024L
    private const val MAX_BACKUP_ENTRY_BYTES = 25L * 1024L * 1024L
    private const val MAX_BACKUP_ENTRIES = 1_024
    private const val MAX_PREFERENCES_BYTES = 256L * 1024L

    /** the manifest of a backup: which app and version wrote it, and when */
    @JsonClass(generateAdapter = true)
    data class Summary(
        val format: Int,
        val app: String,
        @Json(name = "package") val packageName: String?,
        val appVersionName: String?,
        val appVersionCode: Int?,
        val createdAt: String?,
    )

    /** one preference with its type, so it is restored as exactly that type */
    @JsonClass(generateAdapter = true)
    internal data class StoredPreference(val key: String, val type: String, val value: String)

    private val moshi = Moshi.Builder().build()
    private val summaryAdapter = moshi.adapter(Summary::class.java)
    private val preferencesAdapter = moshi.adapter<List<StoredPreference>>(
        Types.newParameterizedType(List::class.java, StoredPreference::class.java)
    )

    fun readSummary(inputStream: InputStream): Summary? {
        ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
            var entryCount = 0
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                if (entryCount > MAX_BACKUP_ENTRIES) {
                    throw IllegalArgumentException("Backup contains too many files")
                }
                if (!entry.isDirectory && entry.name == MANIFEST_ENTRY) {
                    val manifestJson = zip.readEntryTextWithLimit(MAX_PREFERENCES_BYTES)
                    return parseManifest(manifestJson)
                }
                zip.closeEntry()
            }
        }
        return null
    }

    /**
     * Ids of the passes contained in a backup (the directories below "passes/").
     */
    fun passIds(inputStream: InputStream): Set<String> {
        val ids = linkedSetOf<String>()
        ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
            var entryCount = 0
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                if (entryCount > MAX_BACKUP_ENTRIES) {
                    throw IllegalArgumentException("Backup contains too many files")
                }
                val id = entry.name.removePrefix(PASSES_PREFIX).substringBefore('/')
                if (entry.name.startsWith(PASSES_PREFIX) && id.isNotBlank() && id != entry.name.removePrefix(PASSES_PREFIX)) {
                    ids += id
                }
                zip.closeEntry()
            }
        }
        return ids
    }

    fun looksLikePassWalletBackup(inputStream: InputStream): Boolean {
        return try {
            readSummary(inputStream)?.app == BACKUP_APP
        } catch (_: Exception) {
            false
        }
    }

    /** the manifest in [json] - null when it is not a backup manifest (e.g. the manifest of a pkpass file) */
    private fun parseManifest(json: String): Summary? = try {
        summaryAdapter.fromJson(json)
    } catch (_: JsonDataException) {
        null
    } catch (_: IOException) {
        null
    }

    fun exportBackup(
        passesDir: File,
        stateDir: File,
        preferences: Map<String, Any?>,
        appVersionName: String,
        appVersionCode: Int,
        outputStream: OutputStream,
    ) {
        ZipOutputStream(outputStream.buffered()).use { zip ->
            zip.writeTextEntry(
                MANIFEST_ENTRY,
                buildManifest(appVersionName, appVersionCode),
            )
            if (passesDir.exists()) {
                passesDir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        zip.writeFileEntry(file, "passes/${file.relativeTo(passesDir).invariantSeparatorsPath}")
                    }
            }

            val archiveState = File(stateDir, ARCHIVE_STATE_FILE)
            if (archiveState.exists()) {
                zip.writeFileEntry(archiveState, "state/$ARCHIVE_STATE_FILE")
            }

            zip.writeTextEntry(PREFERENCES_ENTRY, encodePreferences(preferences))
        }
    }

    fun importBackup(
        inputStream: InputStream,
        passesDir: File,
        stateDir: File,
        restorePreferences: (Map<String, Any>) -> Unit,
    ) {
        val preferences = mutableMapOf<String, Any>()

        ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
            var entryCount = 0
            var totalBytes = 0L
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                if (entryCount > MAX_BACKUP_ENTRIES) {
                    throw IllegalArgumentException("Backup contains too many files")
                }
                if (!entry.isDirectory) {
                    when {
                        entry.name.startsWith("passes/") -> {
                            val relative = entry.name.removePrefix("passes/")
                            totalBytes += zip.copyEntryToSafeFile(passesDir, relative, MAX_BACKUP_ENTRY_BYTES)
                        }
                        entry.name == "state/$ARCHIVE_STATE_FILE" -> {
                            totalBytes += zip.copyEntryToSafeFile(stateDir, ARCHIVE_STATE_FILE, MAX_BACKUP_ENTRY_BYTES)
                        }
                        entry.name == PREFERENCES_ENTRY -> {
                            val preferencesJson = zip.readEntryTextWithLimit(MAX_PREFERENCES_BYTES)
                            totalBytes += preferencesJson.toByteArray(Charsets.UTF_8).size
                            preferences.putAll(decodePreferences(preferencesJson))
                        }
                    }
                    if (totalBytes > MAX_BACKUP_TOTAL_BYTES) {
                        throw IllegalArgumentException("Backup is too large")
                    }
                }
                zip.closeEntry()
            }
        }

        if (preferences.isNotEmpty()) {
            restorePreferences(preferences)
        }
    }

    private fun buildManifest(appVersionName: String, appVersionCode: Int): String = summaryAdapter.indent("  ").toJson(
        Summary(
            format = 1,
            app = BACKUP_APP,
            packageName = "org.baumweg.passwallet",
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            createdAt = nowIsoUtc(),
        )
    )

    private fun nowIsoUtc(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }

    /** the preferences of the types SharedPreferences can store - others (e.g. string sets) are left out */
    internal fun encodePreferences(preferences: Map<String, Any?>): String {
        val stored = preferences.entries
            .sortedBy { it.key }
            .mapNotNull { (key, value) ->
                val type = when (value) {
                    is String -> "string"
                    is Boolean -> "boolean"
                    is Int -> "int"
                    is Long -> "long"
                    is Float -> "float"
                    else -> return@mapNotNull null
                }
                StoredPreference(key, type, value.toString())
            }
        return preferencesAdapter.indent("  ").toJson(stored)
    }

    /** the preferences in [json] with their original types - entries of unknown type or with broken values are skipped */
    internal fun decodePreferences(json: String): Map<String, Any> {
        val stored = try {
            preferencesAdapter.fromJson(json).orEmpty()
        } catch (_: JsonDataException) {
            emptyList()
        } catch (_: IOException) {
            emptyList()
        }
        return stored.mapNotNull { preference ->
            val value: Any? = when (preference.type) {
                "string" -> preference.value
                "boolean" -> preference.value.toBooleanStrictOrNull()
                "int" -> preference.value.toIntOrNull()
                "long" -> preference.value.toLongOrNull()
                "float" -> preference.value.toFloatOrNull()
                else -> null
            }
            value?.let { preference.key to it }
        }.toMap()
    }

    private fun ZipOutputStream.writeTextEntry(name: String, text: String) {
        putNextEntry(ZipEntry(name))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun ZipOutputStream.writeFileEntry(file: File, name: String) {
        putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(this) }
        closeEntry()
    }

    private fun ZipInputStream.copyEntryToSafeFile(baseDir: File, relativeName: String, byteLimit: Long): Long {
        require(relativeName.isNotBlank()) { "Unsafe empty backup entry" }
        val target = File(baseDir, relativeName)
        val canonicalBase = baseDir.canonicalFile
        val canonicalTarget = target.canonicalFile
        if (!canonicalTarget.path.startsWith(canonicalBase.path + File.separator)) {
            throw IllegalArgumentException("Unsafe backup entry: $relativeName")
        }
        canonicalTarget.parentFile?.mkdirs()
        canonicalTarget.outputStream().use { output ->
            return copyEntryToWithLimit(output, byteLimit)
        }
    }

    private fun ZipInputStream.readEntryTextWithLimit(byteLimit: Long): String {
        val bytes = java.io.ByteArrayOutputStream()
        copyEntryToWithLimit(bytes, byteLimit)
        return bytes.toString(Charsets.UTF_8.name())
    }

    private fun ZipInputStream.copyEntryToWithLimit(output: OutputStream, byteLimit: Long): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            copied += read
            if (copied > byteLimit) {
                throw IllegalArgumentException("Backup entry is too large")
            }
            output.write(buffer, 0, read)
        }
        return copied
    }
}
