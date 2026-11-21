package org.ligi.passandroid.ui

import android.content.Context
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.Settings
import org.ligi.passandroid.ui.PassImportController.FailCallback
import org.ligi.passandroid.ui.PassImportController.SuccessCallback
import java.io.File

open class PassImportSpec(var targetPath: File,
                               val context: Context,
                               val passStore: PassStore,
                               val onSuccessCallback: SuccessCallback?,
                               val failCallback: FailCallback?) {
    var overwrite = false

    constructor(context: Context, passStore: PassStore, onSuccessCallback: SuccessCallback?, failCallback: FailCallback?, settings: Settings)
            : this(settings.getPassesDir(), context, passStore, onSuccessCallback, failCallback)

}
