package org.ligi.passandroid.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * A [PassArchive] that keeps the archive state of all passes in [stateFile].
 */
class FileBackedPassArchive private constructor(
    private val stateFile: File,
    passStore: PassStore,
    private val adapter: JsonAdapter<Map<String, String>>,
) : PassArchive(readStoredState(stateFile, adapter), passStore) {

    constructor(stateFile: File, passStore: PassStore, moshi: Moshi) : this(stateFile, passStore, stateAdapter(moshi))

    /**
     * Takes over the stored state of every pass mentioned in the file (e.g. after a backup was restored);
     * passes not mentioned keep their current state.
     */
    override fun mergeFromStorage() {
        stateById.putAll(readStoredState(stateFile, adapter))
    }

    override fun processDataChange() {
        super.processDataChange()
        try {
            stateFile.parentFile?.mkdirs()
            stateFile.sink().buffer().use { adapter.toJson(it, stateById) }
        } catch (e: IOException) {
            Timber.w(e, "could not write archive state")
        }
    }

    private companion object {
        fun stateAdapter(moshi: Moshi): JsonAdapter<Map<String, String>> =
            moshi.adapter(Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))

        /** the stored state - a missing, unreadable or malformed file results in an empty state */
        fun readStoredState(stateFile: File, adapter: JsonAdapter<Map<String, String>>): MutableMap<String, String> {
            if (stateFile.exists()) {
                try {
                    stateFile.source().buffer().use { source ->
                        adapter.fromJson(source)?.let { return it.toMutableMap() }
                    }
                } catch (e: IOException) {
                    Timber.w(e, "could not read archive state")
                } catch (e: RuntimeException) {
                    Timber.w(e, "could not read archive state")
                }
            }
            return HashMap()
        }
    }
}
