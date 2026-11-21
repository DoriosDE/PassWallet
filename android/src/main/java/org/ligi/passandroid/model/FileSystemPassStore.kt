package org.ligi.passandroid.model

import android.content.Context
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okio.buffer
import okio.sink
import okio.source
import org.ligi.passandroid.BuildConfig
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassVisualClassifier
import org.ligi.passandroid.reader.PkpassReader
import java.io.File

/**
 * Stores every pass in its own directory below [Settings.getPassesDir] (main.json plus images / the original pass files).
 *
 * @param context only used when a Passbook pass is read for the first time (translated field labels)
 * @param language language of the device, used to pick the translation when a Passbook pass is read
 */
class FileSystemPassStore(
        private val context: Context,
        settings: Settings,
        private val moshi: Moshi,
        private val language: () -> String,
) : PassStore {

    private val mutableChanges = MutableSharedFlow<PassStoreChange>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val changes = mutableChanges.asSharedFlow()

    override val directory: File = settings.getPassesDir()

    /** passes read so far - files are only read once, see [reload] / [reloadAll] after changes on disk */
    private val passMap = HashMap<String, Pass>()

    override val archive: PassArchive by lazy {
        val archiveStateFile = File(settings.getStateDir(), ARCHIVE_STATE_FILE)
        FileBackedPassArchive(archiveStateFile, this, moshi, ::notifyChange)
    }

    override fun save(pass: Pass) {
        write(pass)
        notifyChange()
    }

    /** writes main.json of the pass - without announcing it, e.g. when a pass is only brought up to date while read */
    private fun write(pass: Pass) {
        val jsonAdapter = moshi.adapter(Pass::class.java)

        val passDirectory = directoryOf(pass.id)

        if (!passDirectory.exists()) {
            passDirectory.mkdirs()
        }

        val buffer = File(passDirectory, "main.json").sink().buffer()

        if (BuildConfig.DEBUG) {
            val of = com.squareup.moshi.JsonWriter.of(buffer)
            of.indent = "  "
            jsonAdapter.toJson(of, pass)
            buffer.close()
            of.close()
        } else {
            jsonAdapter.toJson(buffer, pass)
            buffer.close()
        }

        passMap[pass.id] = pass
    }

    private fun readPass(id: String): Pass? {
        val passDirectory = directoryOf(id)
        val language = language()

        if (!passDirectory.exists() || !passDirectory.isDirectory) {
            return null
        }

        val file = File(passDirectory, "main.json")
        var result: Pass? = null
        var dirty = true
        if (file.exists()) {
            val jsonAdapter = moshi.adapter(Pass::class.java)
            dirty = false
            try {
                result = jsonAdapter.fromJson(file.source().buffer())
            } catch (ignored: JsonDataException) {
            }
        }

        if (result == null && File(passDirectory, "pass.json").exists()) {
            result = PkpassReader.read(passDirectory, language, context)
        }

        if (result != null) {
            if (updateVisualType(result)) {
                dirty = true
            }
            if (dirty) {
                write(result)
            }
            passMap[id] = result
        }

        return result
    }

    /** the visual type depends on the pass content (e.g. a membership card shown as credential) - kept up to date */
    private fun updateVisualType(pass: Pass): Boolean {
        val classifiedVisualType = PassVisualClassifier.classify(pass)
        if (pass.visualType == classifiedVisualType) return false

        pass.visualType = classifiedVisualType
        return true
    }

    override fun getPass(id: String): Pass? {
        return passMap[id] ?: readPass(id)
    }

    override fun reload(id: String): Pass? {
        passMap.remove(id)
        return readPass(id)
    }

    override fun reloadAll() {
        passMap.clear()
        archive.mergeFromStorage()
        syncArchiveWithStorage()
        notifyChange()
    }

    override fun delete(id: String): Boolean {
        val result = directoryOf(id).deleteRecursively()
        if (result) {
            passMap.remove(id)
            archive.removePass(id)
            notifyChange()
        }
        return result
    }

    override fun directoryOf(id: String): File {
        return File(directory, id)
    }

    private fun notifyChange() {
        mutableChanges.tryEmit(PassStoreChange)
    }

    override fun syncArchiveWithStorage() {
        val existingPassIds = directory.listFiles()?.map { it.name }?.filter { getPass(it) != null }.orEmpty()
        archive.syncWith(existingPassIds)
    }

    companion object {
        /** archive state of all passes, below [Settings.getStateDir] - also part of backups */
        const val ARCHIVE_STATE_FILE = "archive_state.json"
    }
}
