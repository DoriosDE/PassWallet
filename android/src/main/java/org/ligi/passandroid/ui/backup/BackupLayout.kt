package org.ligi.passandroid.ui.backup

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import org.ligi.passandroid.R

/**
 * The views of the backup screen, built in code: a toolbar and two cards, one to export and one to import a backup.
 */
class BackupLayout(
    private val context: Context,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {

    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.color.surface)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    val appBar = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.color.top_app_bar)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    val statusBarSpacer = View(context).apply {
        setBackgroundResource(R.color.top_app_bar)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
    }

    val toolbar = MaterialToolbar(context).apply {
        setBackgroundResource(R.color.top_app_bar)
        setTitleTextColor(context.getColor(android.R.color.white))
        setNavigationIconTint(context.getColor(android.R.color.white))
        popupTheme = R.style.PassWallet_ToolbarPopup
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            context.resources.getDimensionPixelSize(androidx.appcompat.R.dimen.abc_action_bar_default_height_material),
        )
    }

    /** the scrolling content below the toolbar */
    val content = NestedScrollView(context).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
    }

    init {
        appBar.addView(statusBarSpacer)
        appBar.addView(toolbar)
        root.addView(appBar)

        val cards = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        cards.addCard(
            iconRes = R.drawable.ic_backup_export_24,
            title = context.getString(R.string.backup_export_title),
            summary = context.getString(R.string.backup_export_summary),
            buttonText = context.getString(R.string.backup_export_action),
            onClick = onExport,
        )
        cards.addCard(
            iconRes = R.drawable.ic_backup_import_24,
            title = context.getString(R.string.backup_import_title),
            summary = context.getString(R.string.backup_import_summary),
            buttonText = context.getString(R.string.backup_import_action),
            onClick = onImport,
        )
        content.addView(cards)
        root.addView(content)
    }

    private fun LinearLayout.addCard(iconRes: Int, title: String, summary: String, buttonText: String, onClick: () -> Unit) {
        addView(
            createActionCard(iconRes, title, summary, buttonText, onClick),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(16) },
        )
    }

    private fun createActionCard(iconRes: Int, title: String, summary: String, buttonText: String, onClick: () -> Unit): MaterialCardView {
        return MaterialCardView(context).apply {
            radius = dp(22).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(context.getColor(R.color.surface_container))
            strokeColor = context.getColor(R.color.outline_variant)
            strokeWidth = dp(1)
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(20))
                    addView(createIconBadge(iconRes))
                    addView(createText(title, 19f, true).apply {
                        setPadding(0, dp(14), 0, 0)
                    })
                    addView(createText(summary, 14f, false).apply {
                        setTextColor(themeColor(android.R.attr.textColorSecondary))
                        setPadding(0, dp(6), 0, 0)
                    })
                    addView(
                        createActionButton(buttonText, contentDescription = title, onClick),
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                            .apply { topMargin = dp(18) },
                    )
                },
            )
        }
    }

    private fun createIconBadge(iconRes: Int): ImageView {
        return ImageView(context).apply {
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(context.getColor(R.color.edit_action_text))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setColor(context.getColor(R.color.edit_action_background))
                setStroke(dp(1), context.getColor(R.color.edit_action_stroke))
            }
            setPadding(dp(12))
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
        }
    }

    private fun createText(textValue: String, sizeSp: Float, bold: Boolean): TextView {
        return TextView(context).apply {
            text = textValue
            textSize = sizeSp
            setTextColor(themeColor(android.R.attr.textColorPrimary))
            if (bold) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    private fun createActionButton(textValue: String, contentDescription: String, onClick: () -> Unit): MaterialButton {
        return MaterialButton(context).apply {
            text = textValue
            this.contentDescription = contentDescription
            gravity = Gravity.CENTER
            isAllCaps = false
            minimumHeight = dp(52)
            minWidth = 0
            setPadding(dp(18), 0, dp(18), 0)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 15f
            cornerRadius = dp(18)
            rippleColor = ColorStateList.valueOf(context.getColor(R.color.accent))
            setTextColor(context.getColor(android.R.color.white))
            backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.secondary))
            strokeWidth = 0
            setOnClickListener { onClick() }
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private fun themeColor(attr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr, typedValue, true)
        return if (typedValue.resourceId != 0) context.getColor(typedValue.resourceId) else typedValue.data
    }
}
