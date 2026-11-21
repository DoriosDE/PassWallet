package org.ligi.passandroid.unittest

import android.content.Context
import org.ligi.passandroid.json.PassWalletMoshi
import org.ligi.passandroid.model.FileSystemPassStore
import org.ligi.passandroid.model.PassGrouping
import org.ligi.passandroid.model.Settings
import org.ligi.passandroid.model.PassSortOrder
import org.ligi.passandroid.model.pass.Pass
import org.mockito.Mockito.mock
import java.io.File
import org.ligi.passandroid.reader.PkpassReader

/**
 * App storage (passes + state) in a test directory, with real pass stores working on it.
 */
class TestPassDirectory(root: File) {

    val settings = object : Settings {
        override fun getSortOrder() = PassSortOrder.DATE_DESC
        override fun doTraceDroidEmailSend() = false
        override fun getPassesDir() = File(root, "passes")
        override fun getStateDir() = File(root, "state")
        override fun isAutomaticLightEnabled() = false
        override fun isGroupingEnabled() = true
    }

    private val moshi = PassWalletMoshi.create()

    /** a fresh store on this directory - like the app after a restart */
    fun newStore() = FileSystemPassStore(settings, moshi) { passDirectory -> PkpassReader.read(passDirectory, "en", mock(Context::class.java)) }

    companion object {
        fun pass(id: String, description: String) = Pass(id).apply {
            this.description = description
            grouping = PassGrouping.InGroup("group")
        }
    }
}
