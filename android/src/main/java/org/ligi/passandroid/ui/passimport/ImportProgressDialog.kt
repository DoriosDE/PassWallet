package org.ligi.passandroid.ui.passimport

import android.app.Activity
import org.ligi.passandroid.R
import org.ligi.passandroid.importer.BackgroundImport
import org.ligi.passandroid.importer.ImportInput
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.ui.createProgressDialog
import org.ligi.passandroid.ui.dismissSafely

/**
 * Imports while a progress dialog is shown. Returns null when the activity is already finishing.
 */
suspend fun Activity.importWithProgressDialog(passStore: PassStore, input: ImportInput): BackgroundImport.Outcome? {
    if (isFinishing) {
        return null
    }

    val dialog = createProgressDialog(
        getString(R.string.import_pass_dialog_message),
        R.string.import_pass_dialog_title,
    )
    dialog.setCancelable(false)
    dialog.show()

    return try {
        BackgroundImport.run(this, passStore, input)
    } finally {
        dialog.dismissSafely()
    }
}
