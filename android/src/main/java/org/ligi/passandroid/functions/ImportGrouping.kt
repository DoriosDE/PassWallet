package org.ligi.passandroid.functions

import org.ligi.passandroid.model.ArchiveState
import org.ligi.passandroid.model.PassGroups
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.Settings

/**
 * Stacks freshly imported passes onto matching active passes (see [PassGroups.belongTogether]).
 * Returns how many of the imported passes ended up in a group because of this.
 */
fun autoGroupImportedPasses(passStore: PassStore, settings: Settings, importedIds: List<String>): Int {
    if (!settings.isGroupingEnabled()) return 0

    val importedPasses = importedIds.distinct().mapNotNull { passStore.getPassbookForId(it) }
    val activePasses = passStore.archive.passesIn(ArchiveState.ACTIVE)

    val changed = PassGroups.autoGroup(importedPasses, activePasses)
    if (changed.isEmpty()) return 0

    changed.forEach { passStore.save(it) }
    passStore.notifyChange()
    return importedPasses.count { it in changed }
}
