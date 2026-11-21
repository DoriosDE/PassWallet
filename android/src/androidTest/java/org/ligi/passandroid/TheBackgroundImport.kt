package org.ligi.passandroid

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.importer.ImportInput
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.importer.BackgroundImport
import org.mockito.Mockito.mock

class TheBackgroundImport {

    @Test
    fun testShouldFailForBrokenPass() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val inputStream = instrumentation.context.resources.assets.open("passes/broken/fail.pkpass")

        val outcome = runBlocking {
            BackgroundImport.run(
                instrumentation.targetContext,
                mock(PassStore::class.java),
                ImportInput("none", inputStream),
            )
        }

        assertThat(outcome.passIds).isEmpty()
        assertThat(outcome.errorMessages).isNotEmpty()
    }
}
