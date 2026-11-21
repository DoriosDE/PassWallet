package org.ligi.passandroid

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** runs the instrumented tests with [TestApp] instead of the app, so the tests control pass store and settings */
class AppReplacingRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, TestApp::class.java.name, context)
}
