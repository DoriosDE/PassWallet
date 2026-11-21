package org.ligi.passandroid.ui

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.MenuItem
import android.widget.Toast
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.R
import org.ligi.passandroid.functions.confirmAndDeletePasses
import org.ligi.passandroid.functions.setArchived
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.Settings
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.printing.doPrint

class PassMenuOptions(val activity: AppCompatActivity, val pass: Pass) : KoinComponent {

    val passStore: PassStore by inject()
    val settings: Settings by inject()

    fun process(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.menu_archive_pass -> {
                setArchived(passStore.archive, listOf(pass), true)
                Toast.makeText(activity, activity.resources.getQuantityString(R.plurals.passes_archived, 1, 1), Toast.LENGTH_SHORT).show()
                closePassView()
                return true
            }

            R.id.menu_restore_pass -> {
                setArchived(passStore.archive, listOf(pass), false)
                Toast.makeText(activity, activity.resources.getQuantityString(R.plurals.passes_restored, 1, 1), Toast.LENGTH_SHORT).show()
                closePassView()
                return true
            }

            R.id.menu_delete -> {
                activity.confirmAndDeletePasses(passStore, listOf(pass)) { closePassView() }
                return true
            }

            R.id.menu_share -> {
                PassExportTaskAndShare(activity, passStore.getPathForID(pass.id)).execute()
                return true
            }

            R.id.menu_edit -> {
                passStore.currentPass = pass
                activity.startActivity(
                    Intent(activity, PassEditActivity::class.java)
                        .putExtra(PassViewActivityBase.EXTRA_KEY_UUID, pass.id)
                )
                return true
            }

            R.id.menu_print -> {
                doPrint(activity, pass)
                return true
            }
        }
        return false
    }

    /** leave the pass view after the pass was archived, restored or deleted - back to the list it was opened from */
    private fun closePassView() {
        if (activity is PassViewActivityBase) {
            activity.finish()
        }
    }

}
