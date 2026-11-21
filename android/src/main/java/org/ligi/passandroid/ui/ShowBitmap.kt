package org.ligi.passandroid.ui

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import org.ligi.passandroid.R

/**
 * Shows [bitmap] at least a finger high (so small pictures can still be tapped), or hides the view without bitmap.
 */
fun ImageView.showBitmapOrHide(bitmap: Bitmap?) {
    if (bitmap == null) {
        visibility = View.GONE
        return
    }

    setImageBitmap(bitmap)
    visibility = View.VISIBLE
    val fingerSize = resources.getDimensionPixelSize(R.dimen.finger)
    layoutParams = layoutParams!!.apply {
        height = if (bitmap.height < fingerSize) fingerSize else LinearLayout.LayoutParams.WRAP_CONTENT
    }
}
