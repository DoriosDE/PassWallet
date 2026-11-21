package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.reader.PkpassTranslation

class ThePkpassTranslation {

    @Test
    fun testThatNullTranslationWorks() {
        val tested = PkpassTranslation()
        tested.loadFromString("")
        assertThat(tested.translate(null)).isNull()
    }

    @Test
    fun testThatBasicParsingWorks() {
        val tested = PkpassTranslation()
        tested.loadFromString("\"foo\"=\"bar\";")
        assertThat(tested.translate("foo")).isEqualTo("bar")
    }
}
