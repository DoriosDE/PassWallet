package org.ligi.passandroid.importer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.Settings
import java.io.File

/**
 * Connects the screens to the [PassImporter]: imports in the background and returns the result
 * with translated error messages.
 */
object BackgroundImport : KoinComponent {

    private val settings: Settings by inject()

    /** imported pass ids and the (translated) problems found on the way */
    data class Outcome(val passIds: List<String>, val errorMessages: List<String>) {
        /** all problems as one text for an error dialog */
        val errorText: String get() = errorMessages.joinToString("\n\n")
    }

    /**
     * @param overwrite true when updating a pass: an existing pass with the same id is replaced
     * @param passesDir where the passes are stored - only tests use another directory
     */
    suspend fun run(
        context: Context,
        passStore: PassStore,
        input: ImportInput,
        overwrite: Boolean = false,
        passesDir: File = settings.getPassesDir(),
    ): Outcome = withContext(Dispatchers.IO) {
        val importer = PassImporter(
            passesDir = passesDir,
            workDir = context.cacheDir,
            fallbackImport = ImageOrPdfImport(context, passStore),
        )
        val result = importer.import(input.inputStream, input.source, overwrite)
        Outcome(result.passIds, result.errors.map { it.message(context) })
    }

    private fun ImportError.message(context: Context) =
        if (detail != null) context.getString(messageRes, detail) else context.getString(messageRes)
}
