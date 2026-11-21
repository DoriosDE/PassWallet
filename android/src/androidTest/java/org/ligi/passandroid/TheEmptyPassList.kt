package org.ligi.passandroid

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.R.id.emptyView
import org.ligi.passandroid.ui.passlist.PassListActivity
import org.ligi.passandroid.rules.TestIntentRule


class TheEmptyPassList {

    @get:Rule
    var rule = TestIntentRule(PassListActivity::class.java) {
        TestApp.emptyPassStore()
    }

    @Test
    fun testEmptyViewIsThereWhenThereAreNoPasses() {
        rule.screenShot("empty_view")
        onView(withId(emptyView)).check(matches(isDisplayed()))
    }

}
