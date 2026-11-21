package org.ligi.passandroid

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.labelRes
import org.ligi.passandroid.model.pass.PassType

class TheCategoryHelper {

    @Test
    fun testAllCategoriesAreTranslated() {

        val allTranslationSet = PassType.pkpassStyles
                .map { it.labelRes }
                .toSet()

        assertThat(allTranslationSet.size).isEqualTo(PassType.pkpassStyles.size)
    }

}
