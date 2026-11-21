package org.ligi.passandroid.ui

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.RelativeLayout
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.graphics.ColorUtils
import androidx.core.text.util.LinkifyCompat
import org.ligi.passandroid.R
import org.ligi.passandroid.model.pass.BoardingTransitType
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.model.pass.PassVisualClassifier
import org.ligi.passandroid.model.pass.PassVisualType
import org.ligi.passandroid.ui.rendering.BoardingTransitIcon
import org.ligi.passandroid.ui.rendering.PassRenderers

private val CREDENTIAL_PURPLE = Color.rgb(102, 51, 153)

/** the page in pkpass layout: header, primary, secondary and auxiliary fields - for events, boarding passes, store cards and credentials */
class PkpassFragment : PassPageFragment() {

    override fun extrasLayout() = if (ensureCredentialVisualType()) {
        R.layout.pkpass_view_credential_data
    } else {
        R.layout.pkpass_view_extra_data
    }

    override fun onResume() {
        super.onResume()

        val root = requireView()
        val fieldMap : Map<String, ViewGroup> = mapOf(
            "primaryFields" to root.findViewById(R.id.primary_field_container),
            "auxiliaryFields" to root.findViewById(R.id.auxiliary_field_container),
            "headerFields" to root.findViewById(R.id.header_field_container),
            "secondaryFields" to root.findViewById(R.id.secondary_field_container)
        )

        val fieldCount = mutableMapOf<String, Int>()

        fieldMap.forEach {
            fieldCount[it.key] = 0
        }

        val foregroundColor = readableForegroundFor(pass.accentColor)
        val labelColor = ColorUtils.setAlphaComponent(foregroundColor, 210)
        val isCredential = ensureCredentialVisualType()
        val isStoreCard = pass.type == PassType.LOYALTY
        val renderer = PassRenderers.forPass(pass)
        val frontValueColor = if (isCredential) CREDENTIAL_PURPLE else foregroundColor
        val frontLabelColor = if (isCredential) CREDENTIAL_PURPLE else labelColor

        showBarcode(root)
        showPictures(root)
        if (isCredential) {
            applyCredentialLayout(root)
        } else if (isStoreCard) {
            applyStoreCardLayout(root)
        }

        val backStrBuilder = StringBuilder()

        fieldMap.forEach {
            it.value.removeAllViews()
        }

        for (field in pass.fields) {
            val hint = field.hint
            if (field.hide) {
                backStrBuilder.append(field.toHtmlSnippet())
            } else if (hint != null && renderer.showOnDetailFront(field)) {
                val v = requireActivity().layoutInflater.inflate(R.layout.vertical_field_item, root.findViewById(R.id.header_field_container), false)
                val key = v?.findViewById<TextView>(R.id.key)
                val renderedLabel = renderer.detailLabel(field)
                key?.text = renderedLabel
                key?.visibility = if (renderedLabel.isNullOrBlank()) View.GONE else View.VISIBLE
                key?.setTextColor(frontLabelColor)
                val value = v?.findViewById<TextView>(R.id.value)
                value?.text = renderer.detailValue(field)
                value?.setTextColor(frontValueColor)

                if (hint == "headerFields") {
                    if (isCredential) {
                        key?.visibility = View.GONE
                        value?.textSize = 20f
                        value?.gravity = Gravity.RIGHT
                    } else {
                        key?.visibility = View.GONE
                        value?.textSize = 13f
                        value?.gravity = Gravity.RIGHT
                    }
                }

                if (isCredential && hint == "secondaryFields") {
                    key?.textSize = 12f
                    value?.textSize = 22f
                    value?.gravity = Gravity.LEFT
                    key?.gravity = Gravity.LEFT
                }

                if (isCredential && hint == "auxiliaryFields") {
                    key?.textSize = 11f
                    value?.textSize = 17f
                }

                if (isStoreCard && hint == "primaryFields") {
                    value?.textSize = 20f
                    value?.gravity = Gravity.LEFT
                    key?.gravity = Gravity.LEFT
                }

                if (isStoreCard && hint == "secondaryFields") {
                    key?.textSize = 11f
                    value?.textSize = 18f
                    value?.gravity = Gravity.LEFT
                    key?.gravity = Gravity.LEFT
                }

                if (hint == "primaryFields") {
                    value?.textSize = when {
                        pass.type == PassType.EVENT -> 28f
                        isStoreCard -> 20f
                        else -> 32f
                    }
                    key?.textSize = 13f
                    val params = RelativeLayout.LayoutParams(
                        if (pass.type == PassType.EVENT) RelativeLayout.LayoutParams.MATCH_PARENT else RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                    )
                    if (fieldCount[hint]!! == 0) {
                        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                        value?.gravity = Gravity.LEFT
                        key?.gravity = Gravity.LEFT
                        if (pass.type == PassType.EVENT) {
                            value?.maxLines = 1
                            value?.setSingleLine(true)
                        }
                        v?.layoutParams = params
                    } else {
                        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                        value?.gravity = Gravity.RIGHT
                        key?.gravity = Gravity.RIGHT
                        v?.layoutParams = params
                    }
                }
                fieldMap[hint]!!.addView(v)
                key?.let { LinkifyCompat.addLinks(it, LINKIFY_MASK) }
                value?.let { LinkifyCompat.addLinks(it, LINKIFY_MASK) }
                fieldCount[hint] = 1 + fieldCount[hint]!!
            }
        }

        addBoardingTransitIconIfNeeded(
            primaryContainer = fieldMap["primaryFields"],
            primaryFieldCount = fieldCount["primaryFields"] ?: 0,
            foregroundColor = foregroundColor
        )

        showBackFields(root, "$backStrBuilder", if (isStoreCard) R.string.pass_details else R.string.more)

        applyPkPassColors(root, pass.accentColor, foregroundColor, isCredential)
    }

    private fun addBoardingTransitIconIfNeeded(primaryContainer: ViewGroup?, primaryFieldCount: Int, foregroundColor: Int) {
        if (pass.type != PassType.PKBOARDING && pass.type != PassType.BOARDING) return
        if (primaryFieldCount < 2 || primaryContainer !is RelativeLayout) return

        val iconView = ImageView(requireContext()).apply {
            setImageResource(transitIconFor(pass.boardingTransitType))
            setColorFilter(foregroundColor)
            contentDescription = getString(R.string.boarding_transit_icon)
            setPadding(dp(2), dp(2), dp(2), dp(2))
            layoutParams = RelativeLayout.LayoutParams(dp(34), dp(34)).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
        }

        primaryContainer.addView(iconView)
    }

    private fun transitIconFor(transitType: BoardingTransitType): Int {
        return BoardingTransitIcon.drawableFor(transitType)
    }

    private fun ensureCredentialVisualType(): Boolean {
        val classifiedVisualType = PassVisualClassifier.classify(pass)
        if (classifiedVisualType == PassVisualType.CREDENTIAL && pass.visualType != PassVisualType.CREDENTIAL) {
            pass.visualType = PassVisualType.CREDENTIAL
        }
        return pass.visualType == PassVisualType.CREDENTIAL
    }

    private fun applyCredentialLayout(root: View) {
        val logoView = root.findViewById<ImageView>(R.id.logo_img_view)
        logoView.scaleType = ImageView.ScaleType.CENTER_CROP
        logoView.setPadding(0, 0, 0, 0)
        logoView.layoutParams = FrameLayout.LayoutParams(dp(46), dp(58), Gravity.TOP or Gravity.LEFT)

        root.findViewById<ImageView>(R.id.thumbnail_img_view).visibility = View.GONE
        root.findViewById<ImageView>(R.id.footer_img_view).visibility = View.GONE

        val headerContainer = root.findViewById<LinearLayout>(R.id.header_field_container)
        headerContainer.gravity = Gravity.RIGHT
        headerContainer.setPadding(0, 0, 0, 0)

        val stripView = root.findViewById<ImageView>(R.id.strip_img_view)
        stripView.scaleType = ImageView.ScaleType.FIT_CENTER
        stripView.setPadding(0, 0, 0, 0)
        stripView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(150),
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ).apply {
            topMargin = dp(70)
        }

        val barcodeSize = dp(150)
        root.findViewById<ImageView>(R.id.barcode_img).layoutParams = FrameLayout.LayoutParams(
            barcodeSize,
            barcodeSize,
            Gravity.CENTER
        )
        root.findViewById<View>(R.id.barcode_panel).setPadding(0, 0, 0, 0)
    }

    private fun applyStoreCardLayout(root: View) {
        val logoView = root.findViewById<ImageView>(R.id.logo_img_view)
        logoView.scaleType = ImageView.ScaleType.FIT_CENTER
        logoView.setPadding(0, dp(6), dp(6), dp(2))
        logoView.layoutParams = RelativeLayout.LayoutParams(dp(124), dp(56)).apply {
            addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            addRule(RelativeLayout.ALIGN_PARENT_START)
            addRule(RelativeLayout.ALIGN_PARENT_TOP)
        }

        val headerContainer = root.findViewById<LinearLayout>(R.id.header_field_container)
        headerContainer.gravity = Gravity.RIGHT
        headerContainer.setPadding(dp(8), dp(6), 0, 0)

        root.findViewById<ImageView>(R.id.thumbnail_img_view).visibility = View.GONE
        root.findViewById<ImageView>(R.id.strip_img_view).visibility = View.GONE
        root.findViewById<ImageView>(R.id.footer_img_view).visibility = View.GONE

        root.findViewById<RelativeLayout>(R.id.primary_field_container).setPadding(0, dp(10), 0, dp(8))
        root.findViewById<LinearLayout>(R.id.secondary_field_container).setPadding(0, 0, 0, dp(4))
        root.findViewById<LinearLayout>(R.id.auxiliary_field_container).setPadding(0, 0, 0, dp(2))
    }

    private fun applyPkPassColors(root: View, backgroundColor: Int, foregroundColor: Int, isCredential: Boolean) {
        val cardBackground = if (isCredential) Color.WHITE else backgroundColor
        val textColor = if (isCredential) CREDENTIAL_PURPLE else foregroundColor
        root.findViewById<CardView>(R.id.pass_card).setCardBackgroundColor(cardBackground)
        root.findViewById<TextView>(R.id.moreTextView).setTextColor(textColor)
        root.findViewById<TextView>(R.id.back_fields).setTextColor(textColor)
        root.findViewById<TextView>(R.id.barcode_alt_text).setTextColor(textColor)
    }
}
