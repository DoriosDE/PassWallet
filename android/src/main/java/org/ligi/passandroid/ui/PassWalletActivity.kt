package org.ligi.passandroid.ui

import androidx.appcompat.app.AppCompatActivity
import org.koin.android.ext.android.inject
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.Settings

/** base of the screens that work with the stored passes and the settings */
open class PassWalletActivity : AppCompatActivity() {

    val passStore: PassStore by inject()
    val settings: Settings by inject()
}
