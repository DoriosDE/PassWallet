package org.ligi.passandroid.ui

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.core.content.FileProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.R
import org.ligi.passandroid.Tracker
import java.io.File

internal open class PassExportTaskAndShare(
        protected val activity: Activity,
        private val inputPath: File
) : KoinComponent {

    val tracker: Tracker by inject()
    @UiThread
    fun execute() {
        val file = File(activity.filesDir, "share/share.espass") // important - the FileProvider must be configured for this path
        val passExporter = PassExporter(inputPath, file)
        val progressDialog = activity.createProgressDialog(
            activity.getString(R.string.please_wait),
            R.string.preparing_pass,
        )
        progressDialog.show()

        val handler = Handler(Looper.getMainLooper())
        Thread {
            passExporter.export()
            handler.post {
                if (!activity.isFinishing && progressDialog.isShowing) {
                    progressDialog.dismissSafely()
                }

                val exception = passExporter.exception
                if (exception != null) {
                    tracker.trackException("passExporterException", exception, false)
                    val reason = exception.localizedMessage ?: exception.javaClass.simpleName
                    Toast.makeText(activity, activity.getString(R.string.pass_export_error, reason), Toast.LENGTH_LONG).show()
                } else {
                    val uriForFile = FileProvider.getUriForFile(activity, activity.getString(R.string.authority_fileprovider), file)
                    val it = Intent(Intent.ACTION_SEND)
                    it.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.passbook_is_shared_subject))
                    it.putExtra(Intent.EXTRA_STREAM, uriForFile)
                    it.type = "application/vnd.espass-espass"
                    activity.startActivity(Intent.createChooser(it, activity.getString(R.string.passbook_share_chooser_title)))
                }
            }
        }.start()
    }

}
