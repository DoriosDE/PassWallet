package org.ligi.passandroid.update

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassImpl

class ThePassUpdater {

    private fun updatablePass() = PassImpl("id").apply {
        webServiceURL = "https://passes.example.org/api"
        passIdent = "pass.org.example.ticket"
        serial = "12345"
    }

    @Test
    fun aPassWithWebServiceIdentifierAndSerialCanBeUpdated() {
        assertThat(PassUpdater.canUpdate(updatablePass())).isTrue()
    }

    @Test
    fun aPassWithoutAnyOfThemCannotBeUpdated() {
        assertThat(PassUpdater.canUpdate(updatablePass().apply { webServiceURL = null })).isFalse()
        assertThat(PassUpdater.canUpdate(updatablePass().apply { passIdent = null })).isFalse()
        assertThat(PassUpdater.canUpdate(updatablePass().apply { serial = null })).isFalse()
        assertThat(PassUpdater.canUpdate(null)).isFalse()
    }

    @Test
    fun asksTheWebServiceForThePassBySerial() {
        assertThat(PassUpdater.updateUrl(updatablePass()))
            .isEqualTo("https://passes.example.org/api/v1/passes/pass.org.example.ticket/12345")
    }
}
