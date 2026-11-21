package org.ligi.passandroid.functions

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Fail.fail
import org.ligi.passandroid.TestApp
import org.ligi.passandroid.model.InputStreamWithSource
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.reader.AppleStylePassReader
import org.ligi.passandroid.ui.PassImportController
import org.mockito.Mockito.mock
import java.io.File


private fun getTestTargetPath(context: Context) = File(context.cacheDir, "test_passes")


fun loadPassFromAsset(asset: String, callback: (pass: Pass?) -> Unit) {
    try {

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val inputStream = instrumentation.context.resources.assets.open(asset)
        val inputStreamWithSource = InputStreamWithSource("none", inputStream)

        val targetPath = getTestTargetPath(instrumentation.targetContext)
        val outcome = runBlocking {
            PassImportController.import(
                instrumentation.targetContext,
                mock(PassStore::class.java),
                inputStreamWithSource,
                overwrite = true,
                passesDir = targetPath,
            )
        }

        assertThat(outcome.errorMessages).isEmpty()
        val uuid = outcome.passIds.first()
        callback.invoke(AppleStylePassReader.read(File(targetPath, uuid), "en", instrumentation.targetContext, TestApp.tracker))

    } catch (e: Exception) {
        fail("should be able to load file ", e)
    }

}
