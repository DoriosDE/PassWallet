package org.ligi.passandroid.functions

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Fail.fail
import org.ligi.passandroid.importer.ImportInput
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.reader.PkpassReader
import org.ligi.passandroid.importer.BackgroundImport
import org.mockito.Mockito.mock
import java.io.File


private fun getTestTargetPath(context: Context) = File(context.cacheDir, "test_passes")


fun loadPassFromAsset(asset: String, callback: (pass: Pass?) -> Unit) {
    try {

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val inputStream = instrumentation.context.resources.assets.open(asset)
        val importInput = ImportInput("none", inputStream)

        val targetPath = getTestTargetPath(instrumentation.targetContext)
        val outcome = runBlocking {
            BackgroundImport.run(
                instrumentation.targetContext,
                mock(PassStore::class.java),
                importInput,
                overwrite = true,
                passesDir = targetPath,
            )
        }

        assertThat(outcome.errorMessages).isEmpty()
        val uuid = outcome.passIds.first()
        callback.invoke(PkpassReader.read(File(targetPath, uuid), "en", instrumentation.targetContext))

    } catch (e: Exception) {
        fail("should be able to load file ", e)
    }

}
