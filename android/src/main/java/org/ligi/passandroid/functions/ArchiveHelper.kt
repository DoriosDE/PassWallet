package org.ligi.passandroid.functions

import android.app.Activity
import android.view.View
import com.google.android.material.snackbar.Snackbar
import org.ligi.passandroid.R
import org.ligi.passandroid.model.ArchiveState
import org.ligi.passandroid.model.PassClassifier
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.ui.showDestructiveConfirmationDialog

fun Activity.showPassListSnackbar(message: CharSequence, undo: (() -> Unit)? = null) {
    val anchor: View = findViewById(R.id.fam) ?: findViewById(android.R.id.content)
    Snackbar.make(anchor, message, Snackbar.LENGTH_LONG).apply {
        undo?.let { setAction(R.string.undo) { it() } }
    }.show()
}

fun setArchived(passClassifier: PassClassifier, passes: List<Pass>, archived: Boolean) =
    passClassifier.setState(passes, if (archived) ArchiveState.ARCHIVED else ArchiveState.ACTIVE)

fun archiveWithUndoSnackbar(passClassifier: PassClassifier, passes: List<Pass>, activity: Activity) {
    setArchived(passClassifier, passes, true)
    activity.showPassListSnackbar(
        activity.resources.getQuantityString(R.plurals.passes_archived, passes.size, passes.size)
    ) { setArchived(passClassifier, passes, false) }
}

fun restoreWithUndoSnackbar(passClassifier: PassClassifier, passes: List<Pass>, activity: Activity) {
    setArchived(passClassifier, passes, false)
    activity.showPassListSnackbar(
        activity.resources.getQuantityString(R.plurals.passes_restored, passes.size, passes.size)
    ) { setArchived(passClassifier, passes, true) }
}

fun Activity.confirmAndDeletePasses(passStore: PassStore, passes: List<Pass>, onDeleted: () -> Unit = {}) {
    showDestructiveConfirmationDialog(
        titleRes = R.string.delete_passes_dialog_title,
        message = resources.getQuantityString(R.plurals.delete_passes_dialog_message, passes.size, passes.size),
        confirmTextRes = R.string.menu_delete_permanently
    ) {
        passes.forEach { passStore.deletePassWithId(it.id) }
        onDeleted()
    }
}
