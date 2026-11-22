package org.ligi.passandroid.ui.scan

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.google.android.material.button.MaterialButton
import org.ligi.passandroid.R

/**
 * The views of the scanner, built in code: the camera picture with the scan frame,
 * a panel with title and close button on top and the actions (torch, read image, switch camera) at the bottom.
 */
class ScannerLayout(
    private val context: Context,
    onClose: () -> Unit,
    onToggleTorch: () -> Unit,
    onReadImage: () -> Unit,
    onSwitchCamera: () -> Unit,
) {

    val previewView = PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FILL_CENTER
    }

    val root = FrameLayout(context).apply {
        setBackgroundColor(Color.BLACK)
    }

    private val torchButton = scannerButton(R.drawable.ic_flash_24, context.getString(R.string.scan_flash), onClick = onToggleTorch)

    init {
        root.addView(previewView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        root.addView(ScannerOverlayView(context), FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        val topPanel = createTopPanel(onClose)
        root.addView(
            topPanel,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP
                setMargins(18.dp, 18.dp, 18.dp, 0)
            },
        )

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = roundedDrawable(0xCC0B1615.toInt(), 34.dp.toFloat(), 0x33FFFFFF, 1.dp)
            setPadding(10.dp, 10.dp, 10.dp, 10.dp)
        }
        buttons.addView(torchButton)
        buttons.addView(scannerButton(R.drawable.ic_image_24, context.getString(R.string.scan_read_image), 72, onReadImage))
        buttons.addView(scannerButton(R.drawable.ic_camera_rotate_24, context.getString(R.string.scan_rotate_camera), onClick = onSwitchCamera))

        root.addView(
            buttons,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setMargins(18.dp, 0, 18.dp, 28.dp)
            },
        )

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topPanel.updateLayoutParams<FrameLayout.LayoutParams> {
                topMargin = 18.dp + systemBars.top
            }
            buttons.updateLayoutParams<FrameLayout.LayoutParams> {
                bottomMargin = 28.dp + systemBars.bottom
            }
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    /** shows whether the torch is on */
    fun showTorch(enabled: Boolean) {
        val activeColor = if (enabled) 0xFF2DD4BF.toInt() else Color.WHITE
        val backgroundColor = if (enabled) 0x332DD4BF else 0x22FFFFFF
        torchButton.apply {
            setTextColor(activeColor)
            iconTint = ColorStateList.valueOf(activeColor)
            backgroundTintList = ColorStateList.valueOf(backgroundColor)
        }
    }

    fun showPermissionDenied() {
        root.addView(
            TextView(context).apply {
                text = context.getString(R.string.camera_permission_required)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(32.dp, 32.dp, 32.dp, 32.dp)
            },
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
        )
    }

    private fun createTopPanel(onClose: () -> Unit): View {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedDrawable(0xAA0B1615.toInt(), 24.dp.toFloat(), 0x22FFFFFF, 1.dp)
            setPadding(10.dp, 10.dp, 16.dp, 10.dp)
        }

        panel.addView(
            ImageButton(context).apply {
                contentDescription = context.getString(android.R.string.cancel)
                setImageResource(R.drawable.ic_close_24)
                setColorFilter(Color.WHITE)
                background = roundedDrawable(0x33FFFFFF, 20.dp.toFloat())
                scaleType = ImageView.ScaleType.CENTER
                setOnClickListener { onClose() }
            },
            LinearLayout.LayoutParams(40.dp, 40.dp),
        )

        panel.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14.dp, 0, 0, 0)
                addView(TextView(context).apply {
                    text = context.getString(R.string.scan_title)
                    setTextColor(Color.WHITE)
                    textSize = 18f
                    typeface = Typeface.DEFAULT_BOLD
                })
                addView(TextView(context).apply {
                    text = context.getString(R.string.scan_hint)
                    setTextColor(0xCCFFFFFF.toInt())
                    textSize = 13f
                })
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )

        return panel
    }

    private fun scannerButton(@DrawableRes iconRes: Int, text: String, sizeDp: Int = 62, onClick: () -> Unit): MaterialButton {
        return MaterialButton(context).apply {
            contentDescription = text
            this.text = ""
            minWidth = 0
            minHeight = 0
            minimumWidth = 0
            minimumHeight = 0
            gravity = Gravity.CENTER
            icon = ContextCompat.getDrawable(context, iconRes)
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            iconPadding = 0
            iconTint = ColorStateList.valueOf(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(0x22FFFFFF)
            strokeColor = ColorStateList.valueOf(0x33FFFFFF)
            strokeWidth = 1.dp
            cornerRadius = sizeDp.dp / 2
            insetTop = 0
            insetBottom = 0
            setPadding(0, 0, 0, 0)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(sizeDp.dp, sizeDp.dp).apply {
                marginStart = 6.dp
                marginEnd = 6.dp
            }
        }
    }

    private fun roundedDrawable(color: Int, radius: Float, strokeColor: Int? = null, strokeWidth: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(color)
            if (strokeColor != null && strokeWidth > 0) {
                setStroke(strokeWidth, strokeColor)
            }
        }
    }

    private val Int.dp: Int get() = (this * context.resources.displayMetrics.density).toInt()
}
