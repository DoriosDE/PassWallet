package org.ligi.passandroid.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View.GONE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.ligi.kaxt.startActivityFromClass
import org.ligi.passandroid.model.ArchiveState
import org.ligi.passandroid.R
import org.ligi.passandroid.backup.BackupArchive
import org.ligi.passandroid.databinding.ActivityImportBinding
import org.ligi.passandroid.importer.autoGroupImportedPasses
import org.ligi.passandroid.importer.BackgroundImport
import org.ligi.passandroid.importer.ImportInput
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.Settings

class PassImportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportBinding
    val passStore: PassStore by inject()
    val settings: Settings by inject()

    private fun importUri(): Uri? {
        intent.data?.let { return it }

        return when (intent.action) {
            Intent.ACTION_SEND -> intent.extraStreamUri()
            else -> null
        }
    }

    private fun Intent.extraStreamUri(): Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(Intent.EXTRA_STREAM)
    }

    /** imported passes become active and are stacked with matching passes - returns how many were grouped */
    private fun takeOverImportedPasses(passIds: List<String>): Int {
        if (passIds.isEmpty()) return 0
        passStore.archive.setState(passIds.mapNotNull { passStore.getPass(it) }, ArchiveState.ACTIVE)
        return autoGroupImportedPasses(passStore, settings, passIds)
    }

    private fun onImported(outcome: BackgroundImport.Outcome) {
        val groupedCount = takeOverImportedPasses(outcome.passIds)
        if (isFinishing) return

        if (groupedCount > 0) {
            Toast.makeText(
                this,
                resources.getQuantityString(R.plurals.passes_auto_grouped, groupedCount, groupedCount),
                Toast.LENGTH_LONG
            ).show()
        }

        // e.g. broken passes in a collection: the good ones are imported, all problems are shown together
        if (outcome.errorMessages.isNotEmpty()) {
            showImportErrorDialog(
                title = getString(R.string.invalid_passbook_title),
                message = outcome.errorText,
                finishOnOk = true,
            )
            return
        }

        if (outcome.passIds.isEmpty()) {
            finish()
        } else if (outcome.passIds.size == 1) {
            val importedId = outcome.passIds.first()
            passStore.currentPass = passStore.getPass(importedId)
            startActivity(
                Intent(this, PassViewActivity::class.java)
                    .putExtra(PassViewActivityBase.EXTRA_KEY_UUID, importedId)
            )
        } else {
            startActivityFromClass(PassListActivity::class.java)
        }
        finish()
    }

    private fun doImport() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val uri = importUri()
                if (uri == null) {
                    withContext(Dispatchers.Main) { finish() }
                    return@launch
                }

                val input = ImportInput.open(this@PassImportActivity, uri)

                withContext(Dispatchers.Main) {

                    binding.progressContainer.visibility = GONE

                    if (input == null) {
                        showImportFailed(getString(R.string.import_error_file_message))
                    } else if (isPassWalletBackup(uri)) {
                        showImportFailed(getString(R.string.import_error_backup_message))
                    } else {

                        if (isFinishing) {
                            // the screen is already closing: import silently without dialogs
                            withContext(NonCancellable) {
                                val outcome = BackgroundImport.run(application, passStore, input)
                                takeOverImportedPasses(outcome.passIds)
                            }
                        } else {
                            val outcome = importWithProgressDialog(passStore, input)
                            if (outcome != null) {
                                onImported(outcome)
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e // the screen was closed - not an import error
            } catch (e: Exception) {
                if (e.message?.contains("Permission") == true) {
                    withContext(Dispatchers.Main) {
                        onExternalStorageDenied()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.progressContainer.visibility = GONE
                        showImportFailed(getString(R.string.import_error_generic_message, e.localizedMessage ?: e.javaClass.simpleName))
                    }
                }
            }
        }
    }

    private fun isPassWalletBackup(uri: Uri): Boolean {
        return contentResolver.openInputStream(uri)?.use { input ->
            BackupArchive.looksLikePassWalletBackup(input)
        } ?: false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (importUri()?.scheme == null) {
            finish()
            return
        }

        binding = ActivityImportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        doImport()
    }

    private fun onExternalStorageDenied() {
        binding.progressContainer.visibility = GONE
        showImportErrorDialog(
            title = getString(R.string.error_no_permission_title),
            message = getString(R.string.error_no_permission_msg),
            finishOnOk = true,
        )
    }

    private fun showImportFailed(message: String) {
        showImportErrorDialog(
            title = getString(R.string.invalid_passbook_title),
            message = message,
            finishOnOk = true,
        )
    }
}
