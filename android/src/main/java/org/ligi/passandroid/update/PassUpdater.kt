package org.ligi.passandroid.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.ligi.passandroid.importer.BackgroundImport
import org.ligi.passandroid.importer.ImportInput
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Downloads the latest version of a pass from the web service of its provider (pkpass "webServiceURL")
 * and takes it over in the [passStore].
 */
class PassUpdater(
    private val context: Context,
    private val passStore: PassStore,
    private val client: OkHttpClient = defaultClient,
) {

    sealed class Outcome {
        /** the update was imported and taken over - [updatedPass] is null when that failed */
        class Imported(val updatedPass: Pass?, val errorText: String?) : Outcome()
        class Unavailable(val statusCode: Int) : Outcome()
        class NetworkError(val error: IOException) : Outcome()
    }

    suspend fun update(pass: Pass): Outcome = withContext(Dispatchers.IO) {
        val url = updateUrl(pass)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "ApplePass " + pass.authToken)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when (val answer = UpdateResponse.of(response)) {
                    is UpdateResponse.Available -> {
                        val imported = BackgroundImport.run(
                            context,
                            passStore,
                            ImportInput(url, answer.body.byteStream()),
                            overwrite = true,
                        )
                        // the pass store is used by the screens on the main thread - take the update over there
                        val updatedPass = withContext(Dispatchers.Main) {
                            imported.passIds.firstOrNull()?.let { passStore.applyUpdate(pass, it) }
                        }
                        Outcome.Imported(updatedPass, imported.errorText.takeIf { it.isNotEmpty() })
                    }
                    is UpdateResponse.Unavailable -> Outcome.Unavailable(answer.statusCode)
                }
            }
        } catch (e: IOException) {
            Outcome.NetworkError(e)
        }
    }

    companion object {
        /** whether the pass names a web service to ask for updates */
        fun canUpdate(pass: Pass?): Boolean =
            pass?.webServiceURL != null && pass.passIdent != null && pass.serial != null

        internal fun updateUrl(pass: Pass) = pass.webServiceURL + "/v1/passes/" + pass.passIdent + "/" + pass.serial

        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
