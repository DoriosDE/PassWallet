package org.ligi.passandroid.model

/**
 * Whether a pass is shown in the main list or in the archive.
 *
 * @param storedValue how the state is written to the archive state file
 */
enum class ArchiveState(val storedValue: String) {
    ACTIVE("active"),
    ARCHIVED("archived");

    companion object {
        /** anything that is not stored as archived counts as active */
        fun fromStoredValue(value: String?) = if (value == ARCHIVED.storedValue) ARCHIVED else ACTIVE
    }
}
