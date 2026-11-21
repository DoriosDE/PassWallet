package org.ligi.passandroid.ui.passlist

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.ligi.kaxt.startActivityFromClass
import org.ligi.passandroid.R
import org.ligi.passandroid.databinding.PassListBinding
import org.ligi.passandroid.model.createAndAddEmptyPass
import org.ligi.passandroid.model.ArchiveState
import org.ligi.tracedroid.TraceDroid
import org.ligi.tracedroid.sending.sendTraceDroidStackTracesIfExist
import org.ligi.passandroid.ui.PassWalletActivity
import org.ligi.passandroid.ui.applyMaterialInsets
import org.ligi.passandroid.ui.edit.PassEditActivity
import org.ligi.passandroid.ui.passimport.PassImportActivity
import org.ligi.passandroid.ui.settings.PreferenceActivity
import org.ligi.passandroid.ui.showDestructiveConfirmationDialog
import org.ligi.passandroid.ui.EXTRA_PASS_ID

class PassListActivity : PassWalletActivity() {

    private lateinit var binding: PassListBinding
    private var fabMenuExpanded = false

    /** the same screen shows either the active passes or - when started via [showArchive] - the archive */
    private val isArchive by lazy { intent.getBooleanExtra(EXTRA_ARCHIVE, false) }
    private val listState get() = if (isArchive) ArchiveState.ARCHIVED else ArchiveState.ACTIVE
    private val importPassLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            startActivity(Intent(this, PassImportActivity::class.java).setData(it))
        }
    }


    internal fun onImportPassClick() {
        try {
            importPassLauncher.launch(arrayOf("*/*"))
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(binding.fam, R.string.import_unavailable, Snackbar.LENGTH_LONG).show()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = PassListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.emptyView.text = if (isArchive) getString(R.string.archive_empty) else createEmptyText()
        applyMaterialInsets(
            root = binding.root,
            appBar = binding.appbar,
            content = binding.passListContainer,
            floatingView = binding.fam,
        )

        if (isArchive) {
            setTitle(R.string.archive_title)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            binding.fam.visibility = GONE
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.pass_list_container, PassListFragment.newInstance(listState))
                .commit()
        }


        // don't want too many windows in worst case - so check for errors first
        if (TraceDroid.stackTraceFiles.isNotEmpty()) {

            if (settings.doTraceDroidEmailSend()) {
                sendTraceDroidStackTracesIfExist("ligi+passandroid@ligi.de", this)
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            when {
                fabMenuExpanded -> setFabMenuExpanded(false)
                else -> isEnabled = false
            }

            if (!isEnabled) {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        passStore.syncArchiveWithStorage()

        refresh()

        binding.fabActionCreatePass.setOnClickListener {
            val pass = createAndAddEmptyPass(passStore, resources)

            setFabMenuExpanded(false)
            passStore.archive.setState(pass, ArchiveState.ACTIVE)
            startActivity(Intent(this, PassEditActivity::class.java).putExtra(EXTRA_PASS_ID, pass.id))
        }

        binding.fabActionImport.setOnClickListener {
            setFabMenuExpanded(false)
            onImportPassClick()
        }
        binding.fabMenuToggle.setOnClickListener {
            setFabMenuExpanded(!fabMenuExpanded)
        }
        setFabMenuExpanded(false)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                passStore.changes.collect {
                    binding.root.post {
                        if (!isFinishing && !isDestroyed) {
                            refresh()
                        }
                    }
                }
            }
        }
    }

    fun refresh() {
        invalidateOptionsMenu()
        binding.emptyView.visibility = if (currentPasses().isEmpty()) VISIBLE else GONE
    }

    private fun currentPasses() = passStore.archive.passesIn(listState)

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_settings -> {
            startActivityFromClass(PreferenceActivity::class.java)
            true
        }

        R.id.menu_archive -> {
            showArchive()
            true
        }

        R.id.menu_empty_archive -> {
            val archivedPasses = currentPasses()
            showDestructiveConfirmationDialog(
                titleRes = R.string.empty_archive_dialog_title,
                message = resources.getQuantityString(
                    R.plurals.delete_passes_dialog_message,
                    archivedPasses.size,
                    archivedPasses.size,
                ),
                confirmTextRes = R.string.empty_archive_label
            ) {
                for (pass in archivedPasses) {
                    passStore.delete(pass.id)
                }
            }
            true
        }

        android.R.id.home -> {
            finish()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        refresh()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_archive).isVisible = !isArchive
        menu.findItem(R.id.menu_settings).isVisible = !isArchive
        menu.findItem(R.id.menu_empty_archive).isVisible = isArchive && currentPasses().isNotEmpty()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_pass_list_view, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun setFabMenuExpanded(expanded: Boolean) {
        fabMenuExpanded = expanded
        val optionVisibility = if (expanded) VISIBLE else GONE
        binding.fabActionImport.visibility = optionVisibility
        binding.fabActionCreatePass.visibility = optionVisibility
        binding.fabMenuToggle.setImageResource(if (expanded) R.drawable.ic_close_24 else R.drawable.ic_add_24)
        binding.fabMenuToggle.backgroundTintList = getColorStateList(
            if (expanded) R.color.secondary else R.color.accent,
        )
    }

    private fun createEmptyText(): SpannableString {
        val title = getString(R.string.empty_text_title)
        val text = "$title\n\n${getString(R.string.empty_text_body)}"
        return SpannableString(text).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(RelativeSizeSpan(1.15f), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun showArchive() {
        startActivity(Intent(this, PassListActivity::class.java).putExtra(EXTRA_ARCHIVE, true))
    }


    companion object {
        const val EXTRA_ARCHIVE = "archive"
    }
}
