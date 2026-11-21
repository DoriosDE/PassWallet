package org.ligi.passandroid.share

import android.content.Intent
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ligi.passandroid.R
import org.ligi.passandroid.ui.createProgressDialog
import org.ligi.passandroid.ui.dismissSafely
import java.io.File

/**
 * Exports the pass in [passDirectory] to an esPass file in the background and offers to share it.
 */
@UiThread
fun AppCompatActivity.sharePass(passDirectory: File) {
    val file = File(filesDir, "share/share.espass") // important - the FileProvider must be configured for this path
    val progressDialog = createProgressDialog(getString(R.string.please_wait), R.string.preparing_pass)
    progressDialog.show()

    lifecycleScope.launch {
        val exported = try {
            withContext(Dispatchers.IO) { runCatching { exportPass(passDirectory, file) } }
        } finally {
            progressDialog.dismissSafely()
        }

        exported.onSuccess {
            val uriForFile = FileProvider.getUriForFile(this@sharePass, getString(R.string.authority_fileprovider), file)
            val shareIntent = Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.passbook_is_shared_subject))
                .putExtra(Intent.EXTRA_STREAM, uriForFile)
                .setType("application/vnd.espass-espass")
            startActivity(Intent.createChooser(shareIntent, getString(R.string.passbook_share_chooser_title)))
        }.onFailure { exception ->
            val reason = exception.localizedMessage ?: exception.javaClass.simpleName
            Toast.makeText(this@sharePass, getString(R.string.pass_export_error, reason), Toast.LENGTH_LONG).show()
        }
    }
}
