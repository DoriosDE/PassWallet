package org.ligi.passandroid.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.model.pass.PassVisualClassifier
import org.ligi.passandroid.model.pass.PassVisualType

internal const val LINKIFY_MASK = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS

/**
 * One page of the pass view, showing the pass whose id is in the arguments ([PassViewActivityBase.EXTRA_KEY_UUID]).
 * The subclasses lay out the pass - [forPass] picks the one that fits.
 */
abstract class PassPageFragment : Fragment() {

    protected val passStore: PassStore by inject()
    lateinit var pass: Pass

    /** the layout with the extra data shown below the pass card */
    @LayoutRes
    protected abstract fun extrasLayout(): Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.activity_pass_view_page, container, false)
        arguments?.takeIf { it.containsKey(PassViewActivityBase.EXTRA_KEY_UUID) }?.apply {
            val uuid = getString(PassViewActivityBase.EXTRA_KEY_UUID)
            pass = if (uuid != null) {
                passStore.getPass(uuid) ?: passStore.currentPass
            } else {
                passStore.currentPass
            } ?: return rootView
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val passExtrasContainer = view.findViewById<LinearLayout>(R.id.passExtrasContainer)
        passExtrasContainer.addView(layoutInflater.inflate(extrasLayout(), passExtrasContainer, false))
    }

    /**
     * Puts the hidden (back) fields of the pass behind "more", which shows and hides them -
     * [collapsedText] is shown while they are hidden. Without back fields there is no "more".
     */
    protected fun showBackFields(root: View, backFieldsHtml: String, @StringRes collapsedText: Int) {
        val backFields = root.findViewById<TextView>(R.id.back_fields)
        val moreTextView = root.findViewById<TextView>(R.id.moreTextView)
        moreTextView.setOnClickListener {
            if (backFields.isVisible) {
                backFields.visibility = View.GONE
                moreTextView.setText(collapsedText)
            } else {
                backFields.visibility = View.VISIBLE
                moreTextView.setText(R.string.less)
            }
        }

        if (backFieldsHtml.isNotEmpty()) {
            backFields.text = backFieldsHtml.parseAsHtml()
            moreTextView.visibility = View.VISIBLE
            moreTextView.setText(collapsedText)
        } else {
            moreTextView.visibility = View.GONE
        }
        LinkifyCompat.addLinks(backFields, LINKIFY_MASK)
    }

    /** shows the barcode and opens it in fullscreen when tapped */
    protected fun showBarcode(root: View) {
        val openFullscreenBarcode = View.OnClickListener { view ->
            passStore.currentPass = pass
            val intent = Intent(view.context, FullscreenBarcodeActivity::class.java)
            intent.putExtra(PassViewActivityBase.EXTRA_KEY_UUID, pass.id)
            startActivity(intent)
        }
        root.findViewById<View>(R.id.barcode_img).setOnClickListener(openFullscreenBarcode)
        root.findViewById<View>(R.id.barcode_panel).setOnClickListener(openFullscreenBarcode)

        BarcodePanel(root, pass.barCode, requireActivity())
    }

    /**
     * Shows logo, footer, thumbnail and strip of the pass. A simple pass made in the app has no logo:
     * its icon is shown instead. Returns the bitmap shown as logo.
     */
    protected fun showPictures(root: View): Bitmap? {
        val explicitLogoBitmap = pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_LOGO)
        val logoBitmap = explicitLogoBitmap ?: pass.takeIconAsDetailLogoIfManualSimplePass()
        val logoBitmapName = if (explicitLogoBitmap == null && logoBitmap != null) PassBitmapDefinitions.BITMAP_ICON else PassBitmapDefinitions.BITMAP_LOGO
        showPicture(root.findViewById(R.id.logo_img_view), logoBitmapName, logoBitmap)
        showPicture(root.findViewById(R.id.footer_img_view), PassBitmapDefinitions.BITMAP_FOOTER)
        showPicture(root.findViewById(R.id.thumbnail_img_view), PassBitmapDefinitions.BITMAP_THUMBNAIL)
        showPicture(root.findViewById(R.id.strip_img_view), PassBitmapDefinitions.BITMAP_STRIP)
        return logoBitmap
    }

    private fun showPicture(view: ImageView, name: String, bitmap: Bitmap? = pass.getBitmap(passStore, name)) {
        if (bitmap != null && bitmap.width > 300) {
            view.setOnClickListener {
                val intent = Intent(view.context, TouchImageActivity::class.java)
                intent.putExtra("IMAGE", name)
                startActivity(intent)
            }
        } else {
            view.setOnClickListener(null)
        }
        view.showBitmapOrHide(bitmap)
    }

    private fun Pass.takeIconAsDetailLogoIfManualSimplePass(): Bitmap? {
        if (app != "passandroid") return null
        if (getBitmap(passStore, PassBitmapDefinitions.BITMAP_LOGO) != null) return null
        if (getBitmap(passStore, PassBitmapDefinitions.BITMAP_STRIP) != null) return null
        if (getBitmap(passStore, PassBitmapDefinitions.BITMAP_THUMBNAIL) != null) return null
        if (getBitmap(passStore, PassBitmapDefinitions.BITMAP_FOOTER) != null) return null
        if (barCode == null) return null
        if (fields.count { !it.hide } > 2) return null
        return getBitmap(passStore, PassBitmapDefinitions.BITMAP_ICON)
    }

    protected fun readableForegroundFor(backgroundColor: Int): Int {
        return if (ColorUtils.calculateLuminance(backgroundColor) > 0.55) Color.BLACK else Color.WHITE
    }

    protected fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        /**
         * The page for [pass]: credentials and the pkpass styles event, boarding pass and store card get the pkpass layout,
         * everything else the generic one.
         */
        fun forPass(pass: Pass): PassPageFragment {
            if (PassVisualClassifier.classify(pass) == PassVisualType.CREDENTIAL) {
                pass.visualType = PassVisualType.CREDENTIAL
            }

            val fragment = if (pass.visualType == PassVisualType.CREDENTIAL) {
                PkpassFragment()
            } else {
                when (pass.type) {
                    PassType.EVENT,
                    PassType.BOARDING,
                    PassType.PKBOARDING,
                    PassType.LOYALTY -> PkpassFragment()
                    else -> GenericPassFragment()
                }
            }
            fragment.arguments = bundleOf(PassViewActivityBase.EXTRA_KEY_UUID to pass.id)
            return fragment
        }
    }
}
