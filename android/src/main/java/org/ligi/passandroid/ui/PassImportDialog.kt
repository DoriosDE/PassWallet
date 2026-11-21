package org.ligi.passandroid.ui

import android.app.Activity
import org.ligi.passandroid.R
import org.ligi.passandroid.model.InputStreamWithSource
import org.ligi.passandroid.model.PassStore

object PassImportDialog {

    /**
     * Imports while a progress dialog is shown. Returns null when the activity is already finishing.
     */
    suspend fun importWithProgress(
        activity: Activity,
        passStore: PassStore,
        input: InputStreamWithSource,
    ): PassImportController.Outcome? {
        if (activity.isFinishing) {
            return null
        }

        val dialog = activity.createProgressDialog(
            activity.getString(R.string.import_pass_dialog_message),
            R.string.import_pass_dialog_title,
        )
        dialog.setCancelable(false)
        dialog.show()

        return try {
            PassImportController.import(activity, passStore, input)
        } finally {
            dialog.dismissSafely()
        }
    }
}
