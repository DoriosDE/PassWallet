package org.ligi.passandroid.model

import org.ligi.passandroid.model.pass.Pass

/**
 * Knows for every pass whether it is active or archived ([ArchiveState]).
 */
open class PassArchive(protected val stateById: MutableMap<String, String>, private val passStore: PassStore) {

    /** persists and announces a change - overridden to also write the state file */
    protected open fun processDataChange() {
        stateById.values.removeAll { it.isEmpty() }
        passStore.notifyChange()
    }

    /**
     * Takes over the stored state of the passes found in storage, keeping the state of all other passes.
     * Nothing to do for a purely in-memory archive.
     */
    open fun mergeFromStorage() = Unit

    /** ids of all passes the archive knows about */
    val knownPassIds: Set<String> get() = stateById.keys.toSet()

    fun stateOf(pass: Pass): ArchiveState = ArchiveState.fromStoredValue(stateById[pass.id])

    fun isArchived(pass: Pass) = stateOf(pass) == ArchiveState.ARCHIVED

    fun passesIn(state: ArchiveState): List<Pass> = stateById
        .filterValues { ArchiveState.fromStoredValue(it) == state }
        .keys
        .mapNotNull { passStore.getPassbookForId(it) }

    /** moves the passes to [state] - written and announced once for all of them */
    fun setState(passes: Collection<Pass>, state: ArchiveState) {
        passes.forEach { stateById[it.id] = state.storedValue }
        processDataChange()
    }

    fun setState(pass: Pass, state: ArchiveState) = setState(listOf(pass), state)

    /**
     * Matches the archive with the passes that exist: forgets passes that are gone,
     * new passes become active. Only writes and announces something when that changed anything.
     */
    fun syncWith(existingPassIds: Collection<String>) {
        val removedAny = stateById.keys.retainAll(existingPassIds.toSet())
        val newPassIds = existingPassIds.filterNot { it in stateById }
        newPassIds.forEach { stateById[it] = ArchiveState.ACTIVE.storedValue }
        if (removedAny || newPassIds.isNotEmpty()) {
            processDataChange()
        }
    }

    fun removePass(id: String) {
        stateById.remove(id)
        processDataChange()
    }
}
