package org.ligi.passandroid.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.R
import org.ligi.passandroid.databinding.PassGroupItemBinding
import org.ligi.passandroid.model.PassGroups
import org.ligi.passandroid.model.PassListEntry
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.PassStoreProjection
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.ui.pass_view_holder.VerbosePassViewHolder
import org.ligi.passandroid.ui.rendering.PassRenderers

class PassAdapter(
        private val passListActivity: AppCompatActivity,
        private val passStoreProjection: PassStoreProjection
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), KoinComponent {

    private val passStore: PassStore by inject()

    private var entries: List<PassListEntry> = emptyList()

    private var groupingEnabled = false

    init {
        rebuildEntries()
    }

    // the whole list is re-sorted and re-grouped on every change - there are no finer grained change events
    @SuppressLint("NotifyDataSetChanged")
    fun refresh(groupingEnabled: Boolean) {
        this.groupingEnabled = groupingEnabled
        rebuildEntries()
        notifyDataSetChanged()
    }

    private fun rebuildEntries() {
        val passes = passStoreProjection.passList
        entries = if (groupingEnabled) {
            PassGroups.group(passes)
        } else {
            passes.map { PassListEntry.Single(it) }
        }
    }

    override fun getItemViewType(position: Int) = when (entries[position]) {
        is PassListEntry.Single -> VIEW_TYPE_PASS
        is PassListEntry.Group -> VIEW_TYPE_GROUP
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)

        return when (viewType) {
            VIEW_TYPE_GROUP -> GroupViewHolder(PassGroupItemBinding.inflate(inflater, viewGroup, false))
            else -> VerbosePassViewHolder(inflater.inflate(R.layout.pass_list_item, viewGroup, false) as CardView)
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        when (val entry = entries[position]) {
            is PassListEntry.Single -> bindPass(viewHolder as VerbosePassViewHolder, entry.pass)
            is PassListEntry.Group -> bindGroup(viewHolder as GroupViewHolder, entry)
        }
    }

    private fun bindPass(viewHolder: VerbosePassViewHolder, pass: Pass) {
        viewHolder.apply(pass, passStore, passListActivity)
        viewHolder.view.findViewById<TextView>(R.id.groupBadge).visibility = View.GONE

        val root = viewHolder.view

        root.setOnClickListener { openPass(pass) }

        root.setOnLongClickListener {
            PassActionsUI(passListActivity, passStore, listOf(pass), groupingEnabled).show()
            true
        }
    }

    private fun bindGroup(viewHolder: GroupViewHolder, group: PassListEntry.Group) {
        val frontPass = group.passes.first()
        viewHolder.passViewHolder.apply(frontPass, passStore, passListActivity)

        val badge = viewHolder.binding.groupTopCard.groupBadge
        val foregroundColor = foregroundColorFor(frontPass.accentColor)
        badge.visibility = View.VISIBLE
        badge.text = groupCountText(group)
        badge.setTextColor(foregroundColor)
        badge.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(foregroundColor, 38))
        TextViewCompat.setCompoundDrawableTintList(badge, ColorStateList.valueOf(foregroundColor))

        bindPeekingCards(viewHolder, group)

        val card = viewHolder.binding.groupTopCard.root
        card.contentDescription = passListActivity.getString(
            R.string.pass_group_content_description,
            titleOf(frontPass),
            groupCountText(group)
        )
        card.setOnClickListener { openPass(frontPass) }
        card.setOnLongClickListener {
            PassActionsUI(passListActivity, passStore, group.passes, groupingEnabled).show()
            true
        }
    }

    /**
     * The other passes of the group are drawn as cards behind the front card, each one peeking out with its title,
     * so the group looks like an overlapping stack of passes.
     */
    private fun bindPeekingCards(viewHolder: GroupViewHolder, group: PassListEntry.Group) {
        val stack = viewHolder.binding.groupStack
        viewHolder.peekingCards.forEach { stack.removeView(it) }
        viewHolder.peekingCards.clear()

        val resources = passListActivity.resources
        val topMargin = resources.getDimensionPixelSize(R.dimen.pass_stack_top_margin)
        val peekOffset = resources.getDimensionPixelSize(R.dimen.pass_stack_peek_offset)
        val sideMargin = resources.getDimensionPixelSize(R.dimen.pass_stack_side_margin)
        val stackedPasses = group.passes.drop(1).take(MAX_PEEKING_CARDS).reversed()

        stackedPasses.forEachIndexed { index, pass ->
            val peekCard = createPeekingCard(pass).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.pass_stack_peek_card_height)
                ).apply {
                    this.topMargin = topMargin + index * peekOffset
                    marginStart = sideMargin
                    marginEnd = sideMargin
                }
            }
            // add behind the front card - views added earlier are drawn first
            stack.addView(peekCard, index)
            viewHolder.peekingCards.add(peekCard)
        }

        val frontCard = viewHolder.binding.groupTopCard.root
        (frontCard.layoutParams as ViewGroup.MarginLayoutParams).topMargin = topMargin + stackedPasses.size * peekOffset
        frontCard.requestLayout()
    }

    private fun createPeekingCard(pass: Pass): View {
        val context = passListActivity
        val resources = context.resources
        val foregroundColor = foregroundColorFor(pass.accentColor)

        return MaterialCardView(context).apply {
            radius = resources.getDimension(R.dimen.pass_stack_corner_radius)
            cardElevation = 0f
            strokeWidth = resources.getDimensionPixelSize(R.dimen.pass_stack_stroke_width)
            strokeColor = ContextCompat.getColor(context, R.color.outline_variant)
            setCardBackgroundColor(pass.accentColor)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

            addView(TextView(context).apply {
                text = titleOf(pass)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                textSize = 13f
                setTextColor(ColorUtils.setAlphaComponent(foregroundColor, 220))
                gravity = Gravity.CENTER_VERTICAL
                val horizontalPadding = resources.getDimensionPixelSize(R.dimen.pass_stack_peek_padding)
                setPadding(horizontalPadding, 0, horizontalPadding, 0)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.pass_stack_peek_offset)
                )
            })
        }
    }

    private fun foregroundColorFor(color: Int) = if (ColorUtils.calculateLuminance(color) > 0.55) Color.BLACK else Color.WHITE

    private fun titleOf(pass: Pass) = PassRenderers.forPass(pass).listTitle(pass).orEmpty()

    private fun groupCountText(group: PassListEntry.Group) = passListActivity.resources.getQuantityString(
        R.plurals.pass_group_count,
        group.passes.size,
        group.passes.size
    )

    private fun openPass(pass: Pass) {
        passStore.currentPass = pass
        val intent = Intent(passListActivity, PassViewActivity::class.java)
            .putExtra(PassViewActivityBase.EXTRA_KEY_UUID, pass.id)
        passListActivity.startActivity(intent)
    }

    override fun getItemId(position: Int) = position.toLong()
    override fun getItemCount() = entries.size

    private class GroupViewHolder(val binding: PassGroupItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val passViewHolder = VerbosePassViewHolder(binding.groupTopCard.root)
        val peekingCards = ArrayList<View>()
    }

    companion object {
        private const val VIEW_TYPE_PASS = 0
        private const val VIEW_TYPE_GROUP = 1
        private const val MAX_PEEKING_CARDS = 3
    }
}
