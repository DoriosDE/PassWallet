package org.ligi.passandroid.ui.passview

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.MenuItem
import android.widget.Toast
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.R
import org.ligi.passandroid.model.ArchiveState
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.Settings
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.printing.printPass
import org.ligi.passandroid.share.sharePass
import org.ligi.passandroid.ui.edit.PassEditActivity
import org.ligi.passandroid.ui.passlist.confirmAndDeletePasses
import org.ligi.passandroid.ui.EXTRA_PASS_ID

class PassMenuActions(val activity: AppCompatActivity, val pass: Pass) : KoinComponent {

    val passStore: PassStore by inject()
    val settings: Settings by inject()

    fun process(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.menu_archive_pass -> {
                passStore.archive.setState(pass, ArchiveState.ARCHIVED)
                Toast.makeText(activity, activity.resources.getQuantityString(R.plurals.passes_archived, 1, 1), Toast.LENGTH_SHORT).show()
                closePassView()
                return true
            }

            R.id.menu_restore_pass -> {
                passStore.archive.setState(pass, ArchiveState.ACTIVE)
                Toast.makeText(activity, activity.resources.getQuantityString(R.plurals.passes_restored, 1, 1), Toast.LENGTH_SHORT).show()
                closePassView()
                return true
            }

            R.id.menu_delete -> {
                activity.confirmAndDeletePasses(passStore, listOf(pass)) { closePassView() }
                return true
            }

            R.id.menu_share -> {
                activity.sharePass(passStore.directoryOf(pass.id))
                return true
            }

            R.id.menu_edit -> {
                activity.startActivity(
                    Intent(activity, PassEditActivity::class.java)
                        .putExtra(EXTRA_PASS_ID, pass.id)
                )
                return true
            }

            R.id.menu_print -> {
                printPass(activity, pass)
                return true
            }
        }
        return false
    }

    /** leave the pass view after the pass was archived, restored or deleted - back to the list it was opened from */
    private fun closePassView() {
        if (activity is SinglePassActivity) {
            activity.finish()
        }
    }

}
