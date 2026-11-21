package org.ligi.passandroid.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.scale
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.snackbar.Snackbar
import okhttp3.OkHttpClient
import okhttp3.Request
import org.ligi.passandroid.R
import java.util.concurrent.TimeUnit
import org.ligi.passandroid.model.InputStreamWithSource
import org.ligi.passandroid.model.applyUpdate
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_ICON
import org.ligi.passandroid.model.pass.Pass
import java.io.IOException

@SuppressLint("Registered")
open class PassViewActivityBase : PassAndroidActivity() {

    lateinit var currentPass: Pass
    private var fullBrightnessSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // a little hack because I strongly disagree with the style guide here
        // ;-)
        // not having the Actionbar overflow menu also with devices with hardware
        // key really helps discoverability
        // http://stackoverflow.com/questions/9286822/how-to-force-use-of-overflow-menu-on-devices-with-menu-button
        try {
            val config = ViewConfiguration.get(this)
            val menuKeyField = ViewConfiguration::class.java.getDeclaredField("sHasPermanentMenuKey")
            menuKeyField.isAccessible = true
            menuKeyField.setBoolean(config, false)
        } catch (ex: Exception) {
            // Ignore - but at least we tried ;-)
        }

        updateCurrentPass()
    }

    override fun onResume() {
        super.onResume()

        configureActionBar()

        if (settings.isAutomaticLightEnabled()) {
            setToFullBrightness()
        }
    }

    private fun updateCurrentPass() {
        val uuid = intent.getStringExtra(EXTRA_KEY_UUID)

        if (uuid == null) {
            finish()
            return
        }

        val pass = passStore.getPassbookForId(uuid)
        if (pass == null) {
            finish()
            return
        }

        passStore.currentPass = pass
        currentPass = pass
    }

    protected fun hasCurrentPass(): Boolean = ::currentPass.isInitialized

    protected fun configureActionBar() {
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    protected open fun refresh() {
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_pass_view, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val res = super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_light).isVisible = !fullBrightnessSet
        menu.findItem(R.id.menu_print).isVisible = true
        val archived = hasCurrentPass() && passStore.archive.isArchived(currentPass)
        menu.findItem(R.id.menu_archive_pass).isVisible = !archived
        menu.findItem(R.id.menu_restore_pass).isVisible = archived
        menu.findItem(R.id.menu_delete).isVisible = archived
        return res
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (PassMenuOptions(this, currentPass).process(item)) {
            return true
        }

        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            R.id.menu_light -> {
                setToFullBrightness()
                true
            }

            R.id.install_shortcut -> {
                createShortcut()
                true
            }

            R.id.menu_update -> {
                updatePass()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    }

    private fun createShortcut() {
        val passBitmap = currentPass.getBitmap(passStore, BITMAP_ICON)
        val shortcutIcon = passBitmap?.scale(128, 128, filter = true) ?: BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        val name: CharSequence = currentPass.description.let {
            if (it.isNullOrEmpty()) "pass" else it
        }

        val targetIntent = Intent(this, PassViewActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .putExtra(EXTRA_KEY_UUID, currentPass.id)
        val shortcutInfo = ShortcutInfoCompat.Builder(this, "shortcut$name")
            .setIntent(targetIntent)
            .setShortLabel(name)
            .setIcon(IconCompat.createWithBitmap(shortcutIcon))
            .build()
        ShortcutManagerCompat.requestPinShortcut(this, shortcutInfo, null)
    }

    private sealed class UpdateOutcome {
        /** the update was imported and taken over - [updatedPass] is null when that failed */
        class Imported(val updatedPass: Pass?, val errorText: String?) : UpdateOutcome()
        class Unavailable(val statusCode: Int) : UpdateOutcome()
        class NetworkError(val error: IOException) : UpdateOutcome()
    }

    /** downloads the latest version of the pass from its provider and replaces the stored one */
    private fun updatePass() {
        val pass = currentPass
        lifecycleScope.launch {
            val dialog = createProgressDialog(getString(R.string.downloading_new_pass_version))
            dialog.show()
            val outcome = try {
                // the stored pass is changed - finish that even when the screen is closed meanwhile
                withContext(NonCancellable) { downloadAndApplyUpdate(pass) }
            } finally {
                dialog.dismissSafely()
            }
            showUpdateOutcome(outcome)
        }
    }

    private suspend fun downloadAndApplyUpdate(pass: Pass): UpdateOutcome = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        val url = pass.webServiceURL + "/v1/passes/" + pass.passIdent + "/" + pass.serial
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "ApplePass " + pass.authToken)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when (val result = interpretPassUpdateResponse(response)) {
                    is PassUpdateResult.Available -> {
                        val imported = PassImportController.import(
                            this@PassViewActivityBase,
                            passStore,
                            InputStreamWithSource(url, result.body.byteStream()),
                            overwrite = true,
                        )
                        // the pass store is used by the screens on the main thread - take the update over there
                        val updatedPass = withContext(Dispatchers.Main) {
                            imported.passIds.firstOrNull()?.let { passStore.applyUpdate(pass, it) }
                        }
                        UpdateOutcome.Imported(updatedPass, imported.errorText.takeIf { it.isNotEmpty() })
                    }
                    is PassUpdateResult.Unavailable -> UpdateOutcome.Unavailable(result.statusCode)
                }
            }
        } catch (e: IOException) {
            UpdateOutcome.NetworkError(e)
        }
    }

    private fun showUpdateOutcome(outcome: UpdateOutcome) {
        if (isFinishing) return
        when (outcome) {
            is UpdateOutcome.Unavailable -> showImportErrorDialog(
                title = getString(R.string.update_unavailable_title),
                message = getString(R.string.update_unavailable_message, outcome.statusCode),
            )
            is UpdateOutcome.NetworkError -> showUpdateError(
                getString(R.string.update_error_network_message, outcome.error.localizedMessage ?: outcome.error.javaClass.simpleName)
            )
            is UpdateOutcome.Imported -> {
                outcome.updatedPass?.let { updatedPass ->
                    passStore.currentPass = updatedPass
                    currentPass = updatedPass
                    refresh()
                    Snackbar.make(window.decorView, R.string.pass_updated, Snackbar.LENGTH_LONG).show()
                }
                when {
                    outcome.errorText != null -> showUpdateError(getString(R.string.update_error_invalid_message, outcome.errorText))
                    outcome.updatedPass == null -> showUpdateError(
                        getString(R.string.update_error_invalid_message, getString(R.string.update_error_not_saved))
                    )
                }
            }
        }
    }

    private fun showUpdateError(message: String) = showImportErrorDialog(
        title = getString(R.string.update_error_title),
        message = message,
    )

    private fun setToFullBrightness() {
        val win = window
        val params = win.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        win.attributes = params
        fullBrightnessSet = true
        invalidateOptionsMenu()
    }

    companion object {

        const val EXTRA_KEY_UUID = "uuid"

        fun mightPassBeAbleToUpdate(pass: Pass?): Boolean {
            return pass?.webServiceURL != null && pass.passIdent != null && pass.serial != null
        }
    }
}
