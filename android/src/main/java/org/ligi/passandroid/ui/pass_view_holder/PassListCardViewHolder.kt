package org.ligi.passandroid.ui.pass_view_holder

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.ui.rendering.PassListAppearance

/**
 * Binds a pass to the compact wallet card of the pass list (pass_list_item.xml).
 */
class PassListCardViewHolder(val card: MaterialCardView) : RecyclerView.ViewHolder(card) {

    private val logo = card.findViewById<ImageView>(R.id.passLogo)
    private val title = card.findViewById<TextView>(R.id.passTitle)
    private val headerLabel = card.findViewById<TextView>(R.id.headerLabel)
    private val headerValue = card.findViewById<TextView>(R.id.headerValue)

    fun bind(pass: Pass, passStore: PassStore) {
        val appearance = PassListAppearance(pass)

        card.setCardBackgroundColor(appearance.backgroundColor)

        val logoBitmap = pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_LOGO)
        logo.setImageBitmap(logoBitmap)
        logo.visibility = if (logoBitmap != null) View.VISIBLE else View.GONE

        // next to a logo image the pass' own logo text is shown, without logo image the title
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
    }
}
