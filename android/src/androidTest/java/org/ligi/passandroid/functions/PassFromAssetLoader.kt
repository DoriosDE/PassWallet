package org.ligi.passandroid.functions

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Fail.fail
import org.ligi.passandroid.TestApp
import org.ligi.passandroid.model.InputStreamWithSource
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.reader.AppleStylePassReader
import org.ligi.passandroid.ui.PassImportController
import org.ligi.passandroid.ui.PassImportController.FailCallback
import org.ligi.passandroid.ui.PassImportController.InputStreamImportSpec
import org.mockito.Mockito.*
import java.io.File


private fun getTestTargetPath(context: Context) = File(context.cacheDir, "test_passes")


fun loadPassFromAsset(asset: String, callback: (pass: Pass?) -> Unit) {
    try {

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val inputStream = instrumentation.context.resources.assets.open(asset)
        val inputStreamWithSource = InputStreamWithSource("none", inputStream)

        val mock = mock(FailCallback::class.java)
        val spec = InputStreamImportSpec(inputStreamWithSource, instrumentation.targetContext, mock(
                PassStore::class.java),
                object : PassImportController.SuccessCallback {
                    override fun call(uuids: List<String>) {
                        val uuid = uuids.first()
                        callback.invoke(AppleStylePassReader.read(File(getTestTargetPath(instrumentation.targetContext), uuid), "en",
                                instrumentation.targetContext,TestApp.tracker))
                    }
                },
                mock
        )

        spec.overwrite = true
        spec.targetPath = getTestTargetPath(spec.context)
        PassImportController.processInputStream(spec)

        verify(mock, never()).fail(anyString())

    } catch (e: Exception) {
        fail("should be able to load file ", e)
    }

}
