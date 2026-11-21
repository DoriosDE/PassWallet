package org.ligi.passandroid.functions

import android.app.Activity
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import org.ligi.passandroid.R
import org.ligi.passandroid.model.ArchiveState
import org.ligi.passandroid.model.PassArchive
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.ui.showDestructiveConfirmationDialog

fun Activity.showPassListSnackbar(message: CharSequence, undo: (() -> Unit)? = null) {
    val plusButton: View? = findViewById(R.id.fam)
    Snackbar.make(plusButton ?: findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).apply {
        // shown above the plus button, so it never covers it
        if (plusButton?.isVisible == true) anchorView = plusButton
        undo?.let { setAction(R.string.undo) { it() } }
    }.show()
}

fun setArchived(archive: PassArchive, passes: List<Pass>, archived: Boolean) =
    archive.setState(passes, if (archived) ArchiveState.ARCHIVED else ArchiveState.ACTIVE)

fun archiveWithUndoSnackbar(archive: PassArchive, passes: List<Pass>, activity: Activity) {
    setArchived(archive, passes, true)
    activity.showPassListSnackbar(
        activity.resources.getQuantityString(R.plurals.passes_archived, passes.size, passes.size)
    ) { setArchived(archive, passes, false) }
}

fun restoreWithUndoSnackbar(archive: PassArchive, passes: List<Pass>, activity: Activity) {
    setArchived(archive, passes, false)
    activity.showPassListSnackbar(
        activity.resources.getQuantityString(R.plurals.passes_restored, passes.size, passes.size)
    ) { setArchived(archive, passes, true) }
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
