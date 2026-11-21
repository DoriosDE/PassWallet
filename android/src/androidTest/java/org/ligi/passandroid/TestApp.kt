package org.ligi.passandroid

import org.koin.core.module.Module
import org.koin.dsl.module
import org.ligi.passandroid.injections.FixedPassListPassStore
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.ArchiveState
import org.ligi.passandroid.model.Settings
import org.ligi.passandroid.model.PassSortOrder
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File
import java.util.*
import org.ligi.passandroid.ui.EXTRA_PASS_ID
import androidx.test.platform.app.InstrumentationRegistry
import android.content.Intent

class TestApp : App() {

    override fun createKoin(): Module {

        return module {
            single { passStore as PassStore }
            single { settings }
        }
    }

    companion object {

        val passStore = FixedPassListPassStore(emptyList())
        val settings = mock(Settings::class.java).apply {
            `when`(getSortOrder()).thenReturn(PassSortOrder.DATE_ASC)
            `when`(getPassesDir()).thenReturn(File(""))
            `when`(doTraceDroidEmailSend()).thenReturn(false)
        }

        /** stores one pass (with description and barcode) as the only pass and returns it */
        fun populatePassStoreWithSinglePass(): Pass {

            val passList = ArrayList<Pass>()
            val pass = Pass(UUID.randomUUID().toString())
            pass.description = "description"
            pass.barCode = BarCode(PassBarCodeFormat.AZTEC, "messageprobe")
            passList.add(pass)

            fixedPassListPassStore().setList(passList)

            passStore.archive.setState(pass, ArchiveState.ACTIVE)
            return pass
        }

        /** stores [pass] as the only pass */
        fun setSinglePass(pass: Pass) {
            fixedPassListPassStore().setList(listOf(pass))
            passStore.archive.setState(pass, ArchiveState.ACTIVE)
        }

        /** an intent to open [activity] for [pass] */
        fun intentFor(activity: Class<*>, pass: Pass): Intent =
            Intent(InstrumentationRegistry.getInstrumentation().targetContext, activity).putExtra(EXTRA_PASS_ID, pass.id)

        fun emptyPassStore() {
            fixedPassListPassStore().setList(emptyList())
        }

        private fun fixedPassListPassStore() = passStore as FixedPassListPassStore
    }
}
