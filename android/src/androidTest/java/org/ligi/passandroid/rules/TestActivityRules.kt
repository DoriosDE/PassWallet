package org.ligi.passandroid.rules

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import android.view.WindowManager
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.jraska.falcon.Falcon
import java.io.File

/**
 * Starts [activityClass] for each test (unless [launchActivity] is false - then the test calls launchActivity(intent)).
 * [beforeActivityLaunched] prepares the app before the activity starts.
 */
@Suppress("DEPRECATION") // ActivityTestRule: the tests are written against its API
class TestActivityRule<T : Activity>(
    activityClass: Class<T>,
    launchActivity: Boolean = true,
    private val beforeActivityLaunched: () -> Unit = {},
) : ActivityTestRule<T>(activityClass, false, launchActivity) {

    override fun beforeActivityLaunched() = beforeActivityLaunched.invoke()

    override fun afterActivityLaunched() = keepScreenOn(activity)

    /** starts the activity with its default intent */
    fun launchActivity(): T = launchActivity(null)

    fun screenShot(name: String) = takeScreenshot(activity, name)
}

/** like [TestActivityRule], also recording the intents the activity sends (espresso-intents) */
@Suppress("DEPRECATION")
class TestIntentRule<T : Activity>(
    activityClass: Class<T>,
    launchActivity: Boolean = true,
    private val beforeActivityLaunched: () -> Unit = {},
) : IntentsTestRule<T>(activityClass, false, launchActivity) {

    override fun beforeActivityLaunched() = beforeActivityLaunched.invoke()

    override fun afterActivityLaunched() {
        super.afterActivityLaunched()
        keepScreenOn(activity)
    }

    /** starts the activity with its default intent */
    fun launchActivity(): T = launchActivity(null)

    fun screenShot(name: String) = takeScreenshot(activity, name)
}

/** the emulator may lock or turn off its screen during long test runs */
private fun keepScreenOn(activity: Activity) {
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        @Suppress("DEPRECATION")
        activity.window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }
}

/**
 * Saves a screenshot as <name>.png into the additional test output, which Gradle copies to
 * build/outputs/connected_android_test_additional_output after the run (the app files are removed with the app).
 * A screenshot that cannot be taken is only logged - it never fails a test.
 */
private fun takeScreenshot(activity: Activity, name: String) {
    try {
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let { File(it) }
            ?: File(activity.getExternalFilesDir(null), "screenshots")
        directory.mkdirs()
        val bitmap = Falcon.takeScreenshotBitmap(activity)
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    } catch (e: Exception) {
        Log.w("TestActivityRules", "could not take screenshot $name", e)
    }
}
