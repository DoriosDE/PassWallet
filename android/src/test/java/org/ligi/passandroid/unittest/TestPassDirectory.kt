package org.ligi.passandroid.unittest

import android.content.Context
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.json_adapter.PassWalletMoshi
import org.ligi.passandroid.model.AndroidFileSystemPassStore
import org.ligi.passandroid.model.PassGrouping
import org.ligi.passandroid.model.Settings
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.PassImpl
import org.mockito.Mockito.mock
import java.io.File

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
    fun newStore() = AndroidFileSystemPassStore(mock(Context::class.java), settings, moshi, mock(Tracker::class.java)) { "en" }

    companion object {
        fun pass(id: String, description: String) = PassImpl(id).apply {
            this.description = description
            grouping = PassGrouping.InGroup("group")
        }
    }
}
