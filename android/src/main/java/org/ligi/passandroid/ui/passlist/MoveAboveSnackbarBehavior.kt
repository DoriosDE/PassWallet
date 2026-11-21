package org.ligi.passandroid.ui.passlist

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import kotlin.math.min

/**
 * Moves a view (the plus button of the pass list) up while a snackbar is shown, so the snackbar does not cover it.
 */
class MoveAboveSnackbarBehavior : CoordinatorLayout.Behavior<View> {

    constructor() : super()

    @Suppress("unused") // used when the behavior is set in a layout
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View) =
        isSnackbarLayout(dependency)

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        if (isSnackbarLayout(dependency)) {
            child.translationY = min(0f, dependency.translationY - dependency.height)
        }
        return false
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: View, dependency: View) {
        super.onDependentViewRemoved(parent, child, dependency)
        onDependentViewChanged(parent, child, dependency)
    }

    private fun isSnackbarLayout(dependency: View) =
        dependency.javaClass.name == "com.google.android.material.snackbar.Snackbar\$SnackbarLayout"
}
