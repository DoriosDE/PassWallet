package org.ligi.passandroid.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import org.ligi.passandroid.ui.pass_view_holder.PassListCardViewHolder
import org.ligi.passandroid.ui.rendering.PassListAppearance
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
            else -> PassListCardViewHolder(inflater.inflate(R.layout.pass_list_item, viewGroup, false) as MaterialCardView)
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        when (val entry = entries[position]) {
            is PassListEntry.Single -> bindPass(viewHolder as PassListCardViewHolder, entry.pass)
            is PassListEntry.Group -> bindGroup(viewHolder as GroupViewHolder, entry)
        }
    }

    private fun bindPass(viewHolder: PassListCardViewHolder, pass: Pass) {
        viewHolder.bind(pass, passStore)

        val card = viewHolder.card
        card.contentDescription = titleOf(pass)
        card.setOnClickListener { openPass(pass) }
        card.setOnLongClickListener {
            PassActionsUI(passListActivity, passStore, listOf(pass), groupingEnabled).show()
            true
        }
    }

    /**
     * A group is shown as a stack: only the front card (first pass) shows content, the other passes of the group
     * are thin edges peeking out above it.
     */
    private fun bindGroup(viewHolder: GroupViewHolder, group: PassListEntry.Group) {
        val frontPass = group.passes.first()
        viewHolder.frontCard.bind(frontPass, passStore, groupSize = group.passes.size)

        bindStackEdges(viewHolder, group)

        val card = viewHolder.frontCard.card
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

    private fun bindStackEdges(viewHolder: GroupViewHolder, group: PassListEntry.Group) {
        val stack = viewHolder.binding.groupStack
        viewHolder.stackEdges.forEach { stack.removeView(it) }
        viewHolder.stackEdges.clear()

        val resources = passListActivity.resources
        val topMargin = resources.getDimensionPixelSize(R.dimen.pass_card_spacing)
        val edgeOffset = resources.getDimensionPixelSize(R.dimen.pass_stack_edge_offset)
        val edgeInset = resources.getDimensionPixelSize(R.dimen.pass_stack_edge_inset)
        val sideMargin = resources.getDimensionPixelSize(R.dimen.pass_card_side_margin)

        // the pass right behind the front card is drawn last (closest), the farthest one sits on top and is narrowest
        val behindPasses = group.passes.drop(1).take(MAX_STACK_EDGES)
        behindPasses.asReversed().forEachIndexed { index, pass ->
            val depth = behindPasses.size - index
            val edge = createStackEdge(pass).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.pass_card_height)
                ).apply {
                    this.topMargin = topMargin + index * edgeOffset
                    marginStart = sideMargin + depth * edgeInset
                    marginEnd = sideMargin + depth * edgeInset
                }
            }
            // add behind the front card - views added earlier are drawn first
            stack.addView(edge, index)
            viewHolder.stackEdges.add(edge)
        }

        val frontCard = viewHolder.frontCard.card
        (frontCard.layoutParams as ViewGroup.MarginLayoutParams).topMargin = topMargin + behindPasses.size * edgeOffset
        frontCard.requestLayout()
    }

    private fun createStackEdge(pass: Pass): View = MaterialCardView(passListActivity).apply {
        // same shape as a full card - tickets get the notch on every card of the stack
        PassListCardViewHolder.applyShape(this, PassListAppearance(pass).hasTicketNotch)
        cardElevation = 0f
        strokeWidth = resources.getDimensionPixelSize(R.dimen.pass_stack_stroke_width)
        strokeColor = ContextCompat.getColor(context, R.color.outline_variant)
        setCardBackgroundColor(pass.accentColor)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

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
        val frontCard = PassListCardViewHolder(binding.groupTopCard.root)
        val stackEdges = ArrayList<View>()
    }

    companion object {
        private const val VIEW_TYPE_PASS = 0
        private const val VIEW_TYPE_GROUP = 1
        private const val MAX_STACK_EDGES = 3
    }
}
