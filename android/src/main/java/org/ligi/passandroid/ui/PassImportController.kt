package org.ligi.passandroid.ui

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.importer.ImageOrPdfImport
import org.ligi.passandroid.importer.PassImporter
import org.ligi.passandroid.model.InputStreamWithSource
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.Settings

/**
 * Connects the screens to the [PassImporter]: runs an import and reports the result through the callbacks
 * (every problem to the fail callback, the imported passes to the success callback).
 */
object PassImportController : KoinComponent {

    val tracker: Tracker by inject()
    val settings: Settings by inject()

    interface SuccessCallback {
        fun call(uuids: List<String>)
    }

    interface FailCallback {
        fun fail(reason: String)
    }

    fun processInputStream(spec: InputStreamImportSpec) {
        val importer = PassImporter(
            passesDir = spec.targetPath,
            workDir = spec.context.cacheDir,
            fallbackImport = ImageOrPdfImport(spec.context, spec.passStore),
            tracker = tracker,
        )

        val result = importer.import(
            input = spec.inputStreamWithSource.inputStream,
            source = spec.inputStreamWithSource.source,
            overwrite = spec.overwrite,
        )

        result.errors.forEach { error ->
            val message = if (error.detail != null) {
                spec.context.getString(error.messageRes, error.detail)
            } else {
                spec.context.getString(error.messageRes)
            }
            spec.failCallback?.fail(message)
        }
        if (result.passIds.isNotEmpty()) {
            spec.onSuccessCallback?.call(result.passIds)
        }
    }

    class InputStreamImportSpec(
        internal val inputStreamWithSource: InputStreamWithSource,
        context: Context,
        passStore: PassStore,
        onSuccessCallback: SuccessCallback?,
        failCallback: FailCallback?,
    ) : PassImportSpec(context, passStore, onSuccessCallback, failCallback, settings)
}
