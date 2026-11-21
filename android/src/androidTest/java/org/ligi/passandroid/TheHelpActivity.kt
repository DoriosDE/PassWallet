package org.ligi.passandroid

import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.ui.settings.PreferenceActivity
import org.ligi.passandroid.rules.TestActivityRule

class TheHelpActivity {

    @get:Rule
    val rule = TestActivityRule(PreferenceActivity::class.java)

    @Test
    fun testSettingsActivityStarts() {
        rule.screenShot("settings")
    }
}
