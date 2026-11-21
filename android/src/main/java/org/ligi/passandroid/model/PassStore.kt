package org.ligi.passandroid.model

import kotlinx.coroutines.flow.SharedFlow
import org.ligi.passandroid.model.pass.Pass
import java.io.File

interface PassStore {

    val updateChannel: SharedFlow<PassStoreUpdateEvent>

    /** writes the pass and notifies listeners about the change */
    fun save(pass: Pass)

    fun getPassbookForId(id: String): Pass?

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
    fun deletePassWithId(id: String): Boolean

    fun getPathForID(id: String): File

    var currentPass: Pass?

    /** notifies listeners of the store when the archive state of passes changes */
    val archive: PassArchive

    /** matches the archive state with the passes in storage: unknown passes become active, gone passes are forgotten */
    fun syncArchiveWithStorage()
}
