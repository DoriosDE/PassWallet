package org.ligi.passandroid.importer

import androidx.annotation.StringRes

/**
 * Why (part of) an import failed: a translated message, optionally with a technical detail filled into it.
 */
data class ImportError(@StringRes val messageRes: Int, val detail: String? = null)

/**
 * Outcome of an import: the ids of the imported passes and the problems found on the way.
 * A collection of passes can have both - the good passes are imported, every broken one is reported.
 */
data class ImportResult(val passIds: List<String>, val errors: List<ImportError>) {
    companion object {
        fun failed(error: ImportError) = ImportResult(emptyList(), listOf(error))
    }
}
