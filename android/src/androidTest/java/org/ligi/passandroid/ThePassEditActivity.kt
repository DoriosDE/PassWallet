package org.ligi.passandroid

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType.COUPON
import org.ligi.passandroid.model.pass.PassType.EVENT
import org.ligi.passandroid.ui.edit.PassEditActivity
import org.ligi.passandroid.rules.TestIntentRule
import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import org.hamcrest.Matcher
import org.junit.Before
import org.ligi.passandroid.model.pass.Pass

@TargetApi(14)
class ThePassEditActivity {

    val passStore = TestApp.passStore

    private lateinit var pass: Pass

    @get:Rule
    var rule = TestIntentRule(PassEditActivity::class.java, false)

    @Before
    fun before() {
        pass = TestApp.populatePassStoreWithSinglePass()
        rule.launchActivity(TestApp.intentFor(PassEditActivity::class.java, pass))
    }

    @Test
    fun testSetToEventWorks() {
        onView(withId(R.id.categoryView)).perform(click())

        onView(withText(R.string.select_category_dialog_title)).perform(click())
        onView(withText(R.string.category_event)).perform(click())
        assertThat(pass.type).isEqualTo(EVENT)

        rule.screenShot("edit_set_event")
    }

    @Test
    fun testSetToCouponWorks() {
        onView(withId(R.id.categoryView)).perform(click())

        onView(withText(R.string.select_category_dialog_title)).perform(click())
        // on small screens the category sheet opens half way and coupon is below the visible part
        onView(withText(R.string.category_coupon)).perform(clickRowEvenIfOffScreen())
        assertThat(pass.type).isEqualTo(COUPON)

        rule.screenShot("edit_set_coupon")
    }

    @Test
    fun testSetDescriptionWorks() {

        onView(withId(R.id.passTitle)).perform(clearText(), replaceText("test description"))
        assertThat(pass.description).isEqualTo("test description")

        rule.screenShot("edit_set_description")
    }


    @Test
    fun testColorWheelIsThere() {

        onView(withId(R.id.categoryView)).perform(click())
        onView(withText(R.string.change_color_dialog_title)).perform(click())

        onView(withId(R.id.colorPicker)).check(matches(isDisplayed()))

        rule.screenShot("edit_set_color")
    }


    @Test
    fun testAddAbortFooterImagePick() {
        intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))

        onView(withId(R.id.add_footer)).perform(scrollTo(), click())

        intended(hasAction(Intent.ACTION_GET_CONTENT))
    }

    @Test
    fun testAddAbortStripImagePick() {
        intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))

        onView(withId(R.id.add_strip)).perform(scrollTo(), click())

        intended(hasAction(Intent.ACTION_GET_CONTENT))
    }

    @Test
    fun testAddAbortLogoImagePick() {

        intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))

        onView(withId(R.id.add_logo)).perform(scrollTo(), click())

        intended(hasAction(Intent.ACTION_GET_CONTENT))
    }

    /** clicks the clickable row around the view - also when it is outside the visible part of the screen */
    private fun clickRowEvenIfOffScreen() = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isEnabled()
        override fun getDescription() = "click the clickable row around the view"
        override fun perform(uiController: UiController, view: View) {
            generateSequence(view) { it.parent as? View }.first { it.isClickable }.performClick()
            uiController.loopMainThreadUntilIdle()
        }
    }
}
