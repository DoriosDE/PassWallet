package org.ligi.passandroid.ui

import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.ColorUtils
import androidx.core.text.util.LinkifyCompat
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.pass.Pass


/** the page for generic passes, coupons and vouchers: all front fields in one list */
class GenericPassFragment : PassPageFragment() {

    override fun extrasLayout() = R.layout.pass_view_extra_data

    override fun onResume() {
        super.onResume()

        val root = requireView()
        val isStoreCardLike = pass.looksLikeLegacyStoreCard()
        showBarcode(root)

        val logoBitmap = showPictures(root)
        if (isStoreCardLike || logoBitmap != null && pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_LOGO) == null) {
            applyLegacyStoreCardImagePosition(root)
        }

        val backStrBuilder = StringBuilder()
        val foregroundColor = readableForegroundFor(pass.accentColor)
        val secondaryColor = ColorUtils.setAlphaComponent(foregroundColor, 210)

        val front_field_container = root.findViewById<LinearLayout>(R.id.front_field_container)
        front_field_container.removeAllViews()

        for (field in pass.fields) {
            if (field.hide) {
                backStrBuilder.append(field.toHtmlSnippet())
            } else {
                if (isStoreCardLike && field.isGeneratedTypeField()) continue
                val v = requireActivity().layoutInflater.inflate(R.layout.main_field_item, front_field_container, false)
                val key = v?.findViewById<TextView>(R.id.key)
                val value = v?.findViewById<TextView>(R.id.value)
                val fieldLabel = field.label
                val fieldValue = field.value
                val displayValue = if (isStoreCardLike && fieldValue.isNullOrBlank()) fieldLabel else fieldValue
                val displayLabel = if (isStoreCardLike && fieldValue.isNullOrBlank()) null else fieldLabel
                key?.text = displayLabel
                key?.visibility = if (displayLabel.isNullOrBlank()) View.GONE else View.VISIBLE
                key?.setTextColor(secondaryColor)
                value?.text = displayValue
                value?.setTextColor(foregroundColor)

                if (isStoreCardLike && field.key.equals("caduca", ignoreCase = true)) {
                    key?.visibility = View.GONE
                    value?.textSize = 13f
                    value?.gravity = Gravity.RIGHT
                }

                front_field_container.addView(v)
                key?.let { LinkifyCompat.addLinks(it, LINKIFY_MASK) }
                value?.let { LinkifyCompat.addLinks(it, LINKIFY_MASK) }
            }
        }

        showBackFields(root, "$backStrBuilder", if (isStoreCardLike) R.string.pass_details else R.string.more)
        applyGenericPassColors(root, pass.accentColor, foregroundColor)
    }

    private fun applyGenericPassColors(root: View, backgroundColor: Int, foregroundColor: Int) {
        root.findViewById<CardView>(R.id.pass_card).setCardBackgroundColor(backgroundColor)
        root.findViewById<TextView>(R.id.moreTextView).setTextColor(foregroundColor)
        root.findViewById<TextView>(R.id.back_fields).setTextColor(foregroundColor)
        root.findViewById<TextView>(R.id.barcode_alt_text).setTextColor(foregroundColor)
    }

    private fun applyLegacyStoreCardImagePosition(root: View) {
        root.findViewById<ImageView>(R.id.thumbnail_img_view).visibility = View.GONE
        root.findViewById<ImageView>(R.id.strip_img_view).visibility = View.GONE
        root.findViewById<ImageView>(R.id.footer_img_view).visibility = View.GONE

        val logoView = root.findViewById<ImageView>(R.id.logo_img_view)
        val parent = logoView.parent as? LinearLayout ?: return
        parent.removeView(logoView)
        logoView.scaleType = ImageView.ScaleType.FIT_CENTER
        logoView.setPadding(dp(14), dp(10), dp(14), 0)
        logoView.layoutParams = LinearLayout.LayoutParams(dp(150), dp(64)).apply {
            gravity = Gravity.LEFT
        }
        parent.addView(logoView, 0)
    }

    private fun Pass.looksLikeLegacyStoreCard(): Boolean {
        val haystack = buildString {
            append(description.orEmpty()).append(' ')
            append(creator.orEmpty()).append(' ')
            fields.forEach { field ->
                append(field.key.orEmpty()).append(' ')
                append(field.label.orEmpty()).append(' ')
                append(field.value.orEmpty()).append(' ')
            }
        }.lowercase()
        return haystack.contains("renfe") ||
            haystack.contains("numtarjeta") ||
            haystack.contains("nº mas") ||
            haystack.contains("tarjeta plata")
    }

    private fun org.ligi.passandroid.model.pass.PassField.isGeneratedTypeField(): Boolean {
        return key.isNullOrBlank() && label.equals(getString(R.string.type), ignoreCase = true)
    }
}
