package org.ligi.passandroid.ui.passlist

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import org.ligi.passandroid.R
import org.ligi.passandroid.model.ArchiveState
import org.ligi.passandroid.model.PassGrouping
import org.ligi.passandroid.model.PassGroups
import org.ligi.passandroid.model.PassListEntry
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.ui.rendering.PassRenderers

/**
 * Bottom sheet shown on long press in the pass list: archive / restore / delete a pass (or a whole group of passes)
 * and manage pass groups.
 */
internal class PassActionsSheet(
    private val context: Activity,
    private val passStore: PassStore,
    private val passes: List<Pass>,
    private val groupingEnabled: Boolean = true,
) {

    private val pass = passes.first()
    private val isGroup = passes.size > 1
    private val archived = passStore.archive.isArchived(pass)

    fun show() {
        val (dialog, container) = createSheet(
            title = titleOf(pass),
            subtitle = if (isGroup) countText(passes.size) else null
        )

        if (archived) {
            container.addView(createRow(R.drawable.ic_unarchive_24, R.color.accent, context.getString(R.string.menu_restore)) {
                restoreWithUndoSnackbar(passStore.archive, passes, context)
                dialog.dismiss()
            })
            container.addView(createRow(R.drawable.ic_action_delete, R.color.danger, context.getString(R.string.menu_delete_permanently)) {
                dialog.dismiss()
                context.confirmAndDeletePasses(passStore, passes)
            })
        } else {
            container.addView(createRow(R.drawable.ic_archive_24, R.color.accent, context.getString(R.string.menu_archive)) {
                archiveWithUndoSnackbar(passStore.archive, passes, context)
                dialog.dismiss()
            })
        }

        if (groupingEnabled) {
            addGroupActions(container, dialog)
        }
    }

    private fun addGroupActions(container: LinearLayout, dialog: BottomSheetDialog) {
        if (isGroup) {
            container.addView(createRow(R.drawable.ic_pass_ungroup_24, R.color.accent, context.getString(R.string.pass_group_dissolve)) {
                ungroup(passes, R.string.pass_group_dissolved)
                dialog.dismiss()
            })
            return
        }

        if (groupCandidates().isNotEmpty()) {
            container.addView(createRow(R.drawable.ic_pass_group_24, R.color.accent, context.getString(R.string.pass_group_with)) {
                dialog.dismiss()
                showGroupPicker()
            })
        }
        if (pass.grouping.group != null) {
            container.addView(createRow(R.drawable.ic_pass_ungroup_24, R.color.accent, context.getString(R.string.pass_group_remove)) {
                ungroup(listOf(pass), R.string.pass_group_removed)
                dialog.dismiss()
            })
        }
    }

    /**
     * Other passes in the same list (active or archive) this pass could be grouped with.
     * Passes already in the same group are left out.
     */
    private fun groupCandidates(): List<Pass> {
        return passStore.archive.passesIn(passStore.archive.stateOf(pass)).filter { candidate ->
            candidate.id != pass.id && !(pass.grouping.group != null && candidate.grouping == pass.grouping)
        }
    }

    private fun showGroupPicker() {
        val (dialog, container) = createSheet(
            title = context.getString(R.string.pass_group_with),
            subtitle = context.getString(R.string.pass_group_with_subtitle)
        )

        PassGroups.group(groupCandidates()).forEach { entry ->
            val (target, label, icon) = when (entry) {
                is PassListEntry.Single -> Triple(entry.pass, titleOf(entry.pass), R.drawable.ic_ticket_24)
                is PassListEntry.Group -> Triple(
                    entry.passes.first(),
                    context.getString(R.string.pass_group_header, titleOf(entry.passes.first()), countText(entry.passes.size)),
                    R.drawable.ic_pass_group_24
                )
            }
            container.addView(createRow(icon, R.color.accent, label) {
                PassGroups.join(pass, target).forEach { passStore.save(it) }
                context.showPassListSnackbar(context.getString(R.string.pass_grouped))
                dialog.dismiss()
            })
        }
    }

    private fun ungroup(passesToUngroup: List<Pass>, messageRes: Int) {
        val previousGroupings = passesToUngroup.associateWith { it.grouping }
        PassGroups.ungroup(passesToUngroup).forEach { passStore.save(it) }

        context.showPassListSnackbar(context.getString(messageRes)) {
            previousGroupings.forEach { (pass, grouping) ->
                pass.grouping = grouping
                passStore.save(pass)
            }
        }
    }

    private fun createSheet(title: CharSequence, subtitle: CharSequence?): Pair<BottomSheetDialog, LinearLayout> {
        val dialog = BottomSheetDialog(context, R.style.PassWallet_TransparentBottomSheetDialog)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_pass_actions, FrameLayout(context), false)
        dialog.setContentView(view)
        dialog.show()
        makeBottomSheetChromeTransparent(dialog)

        view.findViewById<TextView>(R.id.pass_actions_title).text = title
        view.findViewById<TextView>(R.id.pass_actions_subtitle).apply {
            text = subtitle
            visibility = if (subtitle.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
        return dialog to view.findViewById(R.id.pass_actions_container)
    }

    private fun countText(count: Int) = context.resources.getQuantityString(R.plurals.pass_group_count, count, count)

    private fun titleOf(pass: Pass) = PassRenderers.forPass(pass).listTitle(pass).orEmpty()

    private fun createRow(@DrawableRes iconRes: Int, @ColorRes iconColorRes: Int, label: CharSequence, onClick: () -> Unit): View {
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(6)
                bottomMargin = dp(6)
            }
            radius = dp(18).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = ContextCompat.getColor(context, R.color.edit_action_stroke)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.edit_action_background))
            foreground = ContextCompat.getDrawable(context, selectableItemBackground())
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = context.resources.getDimensionPixelSize(R.dimen.move_topic_row_height)
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }

        val icon = ImageView(context).apply {
            setImageResource(iconRes)
            setColorFilter(ContextCompat.getColor(context, iconColorRes))
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                marginEnd = dp(14)
            }
        }

        val title = TextView(context).apply {
            text = label
            textSize = 16f
            setTypeface(typeface, Typeface.NORMAL)
            setTextColor(ContextCompat.getColor(context, R.color.edit_action_text))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        row.addView(icon)
        row.addView(title)
        card.addView(row)
        return card
    }

    private fun selectableItemBackground(): Int {
        val outValue = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return outValue.resourceId
    }

    private fun makeBottomSheetChromeTransparent(dialog: BottomSheetDialog) {
        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
        var current = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        repeat(5) {
            current?.background = Color.TRANSPARENT.toDrawable()
            current = current?.parent as? View
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
