package org.ligi.passandroid

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.model.ArchiveState
import org.ligi.passandroid.ui.passlist.PassListActivity
import org.ligi.passandroid.rules.TestIntentRule

class ThePassListLongPress {

    @get:Rule
    val rule = TestIntentRule(PassListActivity::class.java) {
        TestApp.populatePassStoreWithSinglePass()
    }

    @Test
    fun testLongPressCanArchivePass() {
        openActionsDialog()

        onView(withText(R.string.menu_archive)).perform(click())

        assertThat(TestApp.passStore.archive.passesIn(ArchiveState.ARCHIVED)).hasSize(1)
        assertThat(TestApp.passStore.archive.passesIn(ArchiveState.ACTIVE)).isEmpty()
    }

    @Test
    fun testLongPressOpensActionsDialog() {
        openActionsDialog()

        rule.screenShot("pass_actions_dialog")
        onView(withText(R.string.menu_archive)).check(matches(isDisplayed()))
    }

    private fun openActionsDialog() {
        onView(withId(R.id.pass_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick())
        )
    }
}
