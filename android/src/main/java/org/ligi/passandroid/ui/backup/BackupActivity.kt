package org.ligi.passandroid.ui.backup

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import org.koin.android.ext.android.inject
import org.ligi.passandroid.BuildConfig
import org.ligi.passandroid.R
import org.ligi.passandroid.backup.BackupArchive
import org.ligi.passandroid.backup.BackupRestore
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.ligi.passandroid.ui.applyMaterialInsets
import org.ligi.passandroid.ui.passlist.PassListActivity
import org.ligi.passandroid.ui.showDestructiveConfirmationDialog

/** exports all passes, their archive state and the settings into a backup file, and restores such a file */
class BackupActivity : AppCompatActivity() {

    private val settings: Settings by inject()
    private val passStore: PassStore by inject()
    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private val exportBackupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { exportBackup(it) }
    }

    private val importBackupLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importBackup(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = BackupLayout(
            this,
            onExport = { exportBackupLauncher.launch(defaultBackupFileName()) },
            onImport = { importBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
        )
        setContentView(layout.root)
        setSupportActionBar(layout.toolbar)
        applyMaterialInsets(
            root = layout.root,
            appBar = layout.appBar,
            content = layout.content,
            statusBarSpacer = layout.statusBarSpacer,
        )
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.backup_title)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun exportBackup(uri: Uri) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(uri)?.use { output ->
                        BackupArchive.exportBackup(
                            passesDir = settings.getPassesDir(),
                            stateDir = settings.getStateDir(),
                            preferences = sharedPreferences.all,
                            appVersionName = BuildConfig.VERSION_NAME,
                            appVersionCode = BuildConfig.VERSION_CODE,
                            outputStream = output,
                        )
                    } ?: error("Could not open output document")
                }
            }
            result.onSuccess {
                Toast.makeText(this@BackupActivity, R.string.backup_export_success, Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(this@BackupActivity, getString(R.string.backup_export_error, it.localizedMessage), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun importBackup(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (ignored: SecurityException) {
            // Some providers grant one-shot access only. That is fine for an immediate restore.
        }

        lifecycleScope.launch {
            val passesNotInBackup = withContext(Dispatchers.IO) {
                runCatching { BackupRestore.passesNotInBackup(passStore) { openBackup(uri) } }
            }.getOrElse {
                showImportError(it)
                return@launch
            }

            showDestructiveConfirmationDialog(
                titleRes = R.string.backup_restore_dialog_title,
                message = getString(R.string.backup_restore_dialog_message),
                confirmTextRes = R.string.backup_restore_action,
                iconRes = R.drawable.ic_backup_import_24,
                checkboxText = passesNotInBackup.size.takeIf { it > 0 }?.let {
                    resources.getQuantityString(R.plurals.backup_restore_remove_others, it, it)
                },
                checkboxChecked = true,
            ) { removeOthers ->
                restoreBackup(uri, removePassesNotInBackup = removeOthers && passesNotInBackup.isNotEmpty())
            }
        }
    }

    private fun restoreBackup(uri: Uri, removePassesNotInBackup: Boolean) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    BackupRestore.restore(
                        openBackup = { openBackup(uri) },
                        passStore = passStore,
                        settings = settings,
                        removePassesNotInBackup = removePassesNotInBackup,
                        restorePreferences = ::restorePreferences,
                    )
                }
            }
            result.onSuccess {
                Toast.makeText(this@BackupActivity, R.string.backup_import_success, Toast.LENGTH_LONG).show()
                startActivity(Intent(this@BackupActivity, PassListActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                finish()
            }.onFailure {
                showImportError(it)
            }
        }
    }

    private fun openBackup(uri: Uri) = contentResolver.openInputStream(uri) ?: error("Could not open input document")

    private fun showImportError(error: Throwable) {
        val message = if (error is BackupRestore.NotABackupException) {
            getString(R.string.backup_import_not_a_backup)
        } else {
            getString(R.string.backup_import_error, error.localizedMessage)
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun restorePreferences(preferences: Map<String, Any>) {
        sharedPreferences.edit {
            preferences.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is String -> putString(key, value)
                }
            }
        }
    }

    private fun defaultBackupFileName(): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "PassWallet-backup-$date.zip"
    }
}
