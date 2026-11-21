package org.ligi.passandroid

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.InputStreamWithSource
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.ui.PassImportController
import org.mockito.Mockito.mock

class ThePassImportController {

    @Test
    fun testShouldFailForBrokenPass() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val inputStream = instrumentation.context.resources.assets.open("passes/broken/fail.pkpass")

        val outcome = runBlocking {
            PassImportController.import(
                instrumentation.targetContext,
                mock(PassStore::class.java),
                InputStreamWithSource("none", inputStream),
            )
        }

        assertThat(outcome.passIds).isEmpty()
        assertThat(outcome.errorMessages).isNotEmpty()
    }
}
