package org.ligi.passandroid.model

import org.ligi.passandroid.model.pass.Pass

/**
 * Passes are either active (shown in the main list) or archived. The state is stored in the
 * [PassClassifier] (which used to hold free-form categories/topics).
 */
object PassArchive {
    /** stored value for active passes - "new" for compatibility with existing installations */
    const val ACTIVE = "new"
    const val ARCHIVE = "archive"

    fun isArchived(classifier: PassClassifier, pass: Pass) = classifier.getTopic(pass, ACTIVE) == ARCHIVE
}
