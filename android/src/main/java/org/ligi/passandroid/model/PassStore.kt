package org.ligi.passandroid.model

import kotlinx.coroutines.flow.SharedFlow
import org.ligi.passandroid.model.pass.Pass
import java.io.File

/** sent by [PassStore.changes] whenever passes or their archive state changed */
object PassStoreChange

interface PassStore {

    /** announces changes of the stored passes, e.g. to refresh lists */
    val changes: SharedFlow<PassStoreChange>

    /** writes the pass and notifies listeners about the change */
    fun save(pass: Pass)

    /** the pass with [id], or null when there is none */
    fun getPass(id: String): Pass?

    /**
     * Reads the pass again from storage, e.g. after its files were replaced by an update.
     * Returns null (and forgets the pass) when it no longer exists.
     */
    fun reload(id: String): Pass?

    /**
     * Reads all passes again from storage, e.g. after a backup was restored into the passes directory.
     * Passes whose archive state is stored get that state, all others keep theirs; new passes become active.
     * Notifies listeners about the change.
     */
    fun reloadAll()

    /** notifies listeners when the pass was deleted */
    fun delete(id: String): Boolean

    /** the directory with the files of the pass (main.json, pictures, original pass files) */
    fun directoryOf(id: String): File

    /** notifies listeners of the store when the archive state of passes changes */
    val archive: PassArchive

    /** matches the archive state with the passes in storage: unknown passes become active, gone passes are forgotten */
    fun syncArchiveWithStorage()
}
