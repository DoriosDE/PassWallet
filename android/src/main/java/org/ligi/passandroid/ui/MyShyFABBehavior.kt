package org.ligi.passandroid.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import org.ligi.passandroid.R
import kotlin.math.max
import kotlin.math.min

/**
 * Moves the floating action button out of the way of a snackbar and hides it while the app bar scrolls away.
 */
class MyShyFABBehavior : CoordinatorLayout.Behavior<View> {

    constructor() : super()

    @Suppress("unused") // used when the behavior is set in a layout
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View) =
        isSnackbarLayout(dependency) || dependency is AppBarLayout

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        if (isSnackbarLayout(dependency)) {
            child.translationY = min(0f, dependency.translationY - dependency.height)
        }
        if (dependency is AppBarLayout) {
            val fabBottomMargin = (child.layoutParams as CoordinatorLayout.LayoutParams).bottomMargin
            val collapsedFabHeight = (56 * child.context.resources.displayMetrics.density).toInt()
            val distanceToScroll = max(child.height, collapsedFabHeight) + fabBottomMargin
            val ratio = dependency.y / toolbarHeight(dependency.context)

            child.translationY = -distanceToScroll * ratio
        }
        return false
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: View, dependency: View) {
        super.onDependentViewRemoved(parent, child, dependency)
        onDependentViewChanged(parent, child, dependency)
    }

    private fun isSnackbarLayout(dependency: View) =
        dependency.javaClass.name == "com.google.android.material.snackbar.Snackbar\$SnackbarLayout"

    private fun toolbarHeight(context: Context): Int {
        val styledAttributes = context.theme.obtainStyledAttributes(intArrayOf(R.attr.actionBarSize))
        val toolbarHeight = styledAttributes.getDimension(0, 0f).toInt()
        styledAttributes.recycle()
        return toolbarHeight
    }
}
