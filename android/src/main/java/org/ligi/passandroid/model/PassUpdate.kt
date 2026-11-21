package org.ligi.passandroid.model

import org.ligi.passandroid.model.pass.Pass

/**
 * Takes over a pass update that was imported under [updatedPassId]: removes the [previous] pass when the id
 * changed, reads the new version and keeps it in the group of the previous one.
 * Returns the updated pass, or null when it was not stored.
 */
fun PassStore.applyUpdate(previous: Pass, updatedPassId: String): Pass? {
    val previousGrouping = previous.grouping
    if (previous.id != updatedPassId) {
        deletePassWithId(previous.id)
    }
    val updated = reload(updatedPassId) ?: return null
    if (PassGroups.keepGroupingAfterUpdate(previousGrouping, updated)) {
        save(updated)
    }
    return updated
}
