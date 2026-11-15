package org.ligi.passandroid.ui.pass_view_holder

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.ShapeAppearanceModel
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.ui.rendering.PassListAppearance
import org.ligi.passandroid.ui.views.TicketNotchEdgeTreatment

/**
 * Binds a pass to the compact wallet card of the pass list (pass_list_item.xml).
 */
class PassListCardViewHolder(val card: MaterialCardView) : RecyclerView.ViewHolder(card) {

    private val logo = card.findViewById<ImageView>(R.id.passLogo)
    private val title = card.findViewById<TextView>(R.id.passTitle)
    private val badge = card.findViewById<TextView>(R.id.groupBadge)
    private val headerLabel = card.findViewById<TextView>(R.id.headerLabel)
    private val headerValue = card.findViewById<TextView>(R.id.headerValue)

    /**
     * @param groupSize number of passes in the stack this card is the front of, null for a single pass
     */
    fun bind(pass: Pass, passStore: PassStore, groupSize: Int? = null) {
        val appearance = PassListAppearance(pass)

        applyShape(card, appearance.hasTicketNotch)
        card.setCardBackgroundColor(appearance.backgroundColor)

        val logoBitmap = pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_LOGO)
        logo.setImageBitmap(logoBitmap)
        logo.visibility = if (logoBitmap != null) View.VISIBLE else View.GONE

        // next to a logo image only the pass' own logo text is shown, without logo image the logo text or title
        val leftText = if (logoBitmap != null) appearance.logoText else appearance.fallbackText
        title.text = leftText
        title.visibility = if (leftText.isNullOrEmpty()) View.GONE else View.VISIBLE
        title.setTextColor(appearance.foregroundColor)
        title.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginStart = if (logoBitmap != null) card.resources.getDimensionPixelSize(R.dimen.pass_card_logo_spacing) else 0
        }

        headerLabel.text = appearance.headerLabel
        headerLabel.visibility = if (appearance.headerLabel.isNullOrEmpty()) View.GONE else View.VISIBLE
        headerLabel.setTextColor(appearance.labelColor)
        headerValue.text = appearance.headerValue
        headerValue.visibility = if (appearance.headerValue.isNullOrEmpty()) View.GONE else View.VISIBLE
        headerValue.setTextColor(appearance.foregroundColor)

        if (groupSize != null) {
            badge.visibility = View.VISIBLE
            badge.text = "$groupSize"
            badge.setTextColor(appearance.foregroundColor)
            badge.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(appearance.foregroundColor, 38))
            TextViewCompat.setCompoundDrawableTintList(badge, ColorStateList.valueOf(appearance.foregroundColor))
        } else {
            badge.visibility = View.GONE
        }
    }

    companion object {
        /** rounded card, with a punched notch at the top for tickets */
        fun applyShape(card: MaterialCardView, ticketNotch: Boolean) {
            val resources = card.resources
            val builder = ShapeAppearanceModel.builder()
                .setAllCornerSizes(resources.getDimension(R.dimen.pass_card_corner_radius))
            if (ticketNotch) {
                builder.setTopEdge(
                    TicketNotchEdgeTreatment(
                        resources.getDimension(R.dimen.pass_card_notch_width),
                        resources.getDimension(R.dimen.pass_card_notch_depth)
                    )
                )
            }
            card.shapeAppearanceModel = builder.build()
        }
    }
}
