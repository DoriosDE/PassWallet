package org.ligi.passandroid

import android.Manifest
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.linkedin.android.testbutler.TestButler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.model.AndroidSettings
import org.ligi.passandroid.model.PassSortOrder
import org.ligi.passandroid.ui.settings.PreferenceActivity
import org.ligi.trulesk.TruleskActivityRule

class ThePreferenceActivity {

    @get:Rule
    val rule = TruleskActivityRule(PreferenceActivity::class.java) {
        TestButler.grantPermission(ApplicationProvider.getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
        TestButler.grantPermission(ApplicationProvider.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private val androidSettings by lazy { AndroidSettings(rule.activity) }

    @Test
    fun autoLightToggles() {
        rule.screenShot("preferences")

        val automaticLightEnabled = androidSettings.isAutomaticLightEnabled()

        onView(withText(R.string.preference_autolight_title)).perform(click())

        assertThat(automaticLightEnabled).isEqualTo(!androidSettings.isAutomaticLightEnabled())

        onView(withText(R.string.preference_autolight_title)).perform(click())

        assertThat(automaticLightEnabled).isEqualTo(androidSettings.isAutomaticLightEnabled())

    }

    @Test
    fun weCanSetAllSortOrders() {

        val resources = rule.activity.resources
        val sortOrders = resources.getStringArray(R.array.sort_orders)
        sortOrders.forEach { sortOrder ->

            onView(withText(R.string.preference_sort_title)).perform(click())
            onView(withText(sortOrder)).perform(click())

            assertThat(androidSettings.getSortOrder()).isEqualTo(when (sortOrder) {
                resources.getString(R.string.sort_order_date_asc) -> PassSortOrder.DATE_ASC
                resources.getString(R.string.sort_order_date_desc) -> PassSortOrder.DATE_DESC
                resources.getString(R.string.sort_order_date_type) -> PassSortOrder.TYPE
                resources.getString(R.string.sort_order_date_temporaldistance) -> PassSortOrder.DATE_DIFF
                else -> throw RuntimeException("unexpected sort order")
            })
        }
    }
}
