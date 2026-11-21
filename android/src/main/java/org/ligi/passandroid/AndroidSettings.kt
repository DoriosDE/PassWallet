package org.ligi.passandroid

import android.content.Context
import androidx.preference.PreferenceManager
import org.ligi.passandroid.R.string.preference_key_autolight
import org.ligi.passandroid.model.PassSortOrder
import org.ligi.passandroid.model.Settings
import java.io.File

/** the [Settings] kept in the default shared preferences of the app */
class AndroidSettings(val context: Context) : Settings {

    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    override fun getSortOrder(): PassSortOrder {
        val key = context.getString(R.string.preference_key_sort)
        return PassSortOrder.fromPreferenceValue(sharedPreferences.getString(key, null)?.toIntOrNull())
    }

    override fun doTraceDroidEmailSend() = true

    override fun getPassesDir() = File(context.filesDir.absolutePath, "passes")

    override fun getStateDir() = File(context.filesDir, "state")

    override fun isAutomaticLightEnabled() = sharedPreferences.getBoolean(context.getString(preference_key_autolight), true)

    override fun isGroupingEnabled() = sharedPreferences.getBoolean(context.getString(R.string.preference_key_grouping), true)

}
