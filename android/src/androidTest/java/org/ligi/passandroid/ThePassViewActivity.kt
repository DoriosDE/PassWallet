package org.ligi.passandroid

import android.annotation.TargetApi
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.core.IsNot.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassLocation
import org.ligi.passandroid.ui.passview.PassViewActivity
import org.ligi.passandroid.rules.TestActivityRule
import java.util.ArrayList

@TargetApi(14)
class ThePassViewActivity {

    private lateinit var pass: Pass

    private fun getActPass() = pass

    @get:Rule
    var rule = TestActivityRule(PassViewActivity::class.java, false)

    @Before
    fun before() {
        pass = TestApp.populatePassStoreWithSinglePass()
    }

    @Test
    fun testPassDetailCardIsThere() {
        rule.launchActivity(TestApp.intentFor(PassViewActivity::class.java, pass))

        onView(withId(R.id.pass_card)).check(matches(isDisplayed()))
        onView(withId(R.id.passExtrasContainer)).check(matches(isDisplayed()))
    }

    @Test
    fun testListHeaderIsNotRenderedInPassDetail() {
        rule.launchActivity(TestApp.intentFor(PassViewActivity::class.java, pass))

        onView(withId(R.id.pass_top)).check(doesNotExist())
        onView(withId(R.id.categoryView)).check(doesNotExist())
        onView(withId(R.id.date)).check(doesNotExist())
        onView(withId(R.id.passTitle)).check(doesNotExist())
    }

    @Test
    fun testEverythingWorksWhenWeHaveSomeLocation() {
        val locations = ArrayList<PassLocation>()
        locations.add(PassLocation())
        getActPass().locations = locations
        rule.launchActivity(TestApp.intentFor(PassViewActivity::class.java, pass))

        onView(withId(R.id.pass_card)).check(matches(isDisplayed()))
        onView(withId(R.id.pass_top)).check(doesNotExist())
    }

    @Test
    fun testClickOnBarcodeOpensFullscreenImage() {
        getActPass().barCode = BarCode(PassBarCodeFormat.QR_CODE, "foo")
        rule.launchActivity(TestApp.intentFor(PassViewActivity::class.java, pass))
        onView(withId(R.id.barcode_img)).perform(click())

        onView(withId(R.id.fullscreen_barcode)).check(matches(isDisplayed()))
    }

    @Test
    fun testZoomControlsAreNotShown() {
        getActPass().barCode = BarCode(PassBarCodeFormat.AZTEC, "foo")
        rule.launchActivity(TestApp.intentFor(PassViewActivity::class.java, pass))

        onView(withId(R.id.zoomIn)).check(matches(not(isDisplayed())))
        onView(withId(R.id.zoomOut)).check(matches(not(isDisplayed())))
    }
}
