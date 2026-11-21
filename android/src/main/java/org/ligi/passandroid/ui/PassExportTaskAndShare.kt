package org.ligi.passandroid.ui

import android.content.Intent
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.R
import org.ligi.passandroid.Tracker
import java.io.File

/**
 * Exports a pass to an esPass file in the background and offers to share it.
 */
internal class PassExportTaskAndShare(
        private val activity: AppCompatActivity,
        private val inputPath: File
) : KoinComponent {

    private val tracker: Tracker by inject()

    @UiThread
    fun execute() {
        val file = File(activity.filesDir, "share/share.espass") // important - the FileProvider must be configured for this path
        val passExporter = PassExporter(inputPath, file)
        val progressDialog = activity.createProgressDialog(
            activity.getString(R.string.please_wait),
            R.string.preparing_pass,
        )
        progressDialog.show()

        activity.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { passExporter.export() }
            } finally {
                progressDialog.dismissSafely()
            }

            val exception = passExporter.exception
            if (exception != null) {
                tracker.trackException("passExporterException", exception, false)
                val reason = exception.localizedMessage ?: exception.javaClass.simpleName
                Toast.makeText(activity, activity.getString(R.string.pass_export_error, reason), Toast.LENGTH_LONG).show()
            } else {
                val uriForFile = FileProvider.getUriForFile(activity, activity.getString(R.string.authority_fileprovider), file)
                val shareIntent = Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.passbook_is_shared_subject))
                    .putExtra(Intent.EXTRA_STREAM, uriForFile)
                    .setType("application/vnd.espass-espass")
                activity.startActivity(Intent.createChooser(shareIntent, activity.getString(R.string.passbook_share_chooser_title)))
            }
        }
    }
}
