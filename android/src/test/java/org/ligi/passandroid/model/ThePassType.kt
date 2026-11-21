package org.ligi.passandroid.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType

class ThePassType {

    @Test
    fun everyTypeWithPkpassStyleKeyIsReadFromPkpass() {
        assertThat(PassType.pkpassStyles.toSet())
            .isEqualTo(PassType.entries.filter { it.pkpassStyleKey != null }.toSet())
    }
}
