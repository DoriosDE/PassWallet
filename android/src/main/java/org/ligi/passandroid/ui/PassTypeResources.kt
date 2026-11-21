package org.ligi.passandroid.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.ligi.passandroid.R
import org.ligi.passandroid.model.pass.PassType

/** the name of the pass type as shown to the user */
@get:StringRes
val PassType.labelRes: Int
    get() = when (this) {
        PassType.BOARDING -> R.string.boarding_pass
        PassType.PKBOARDING -> R.string.boarding_pass
        PassType.EVENT -> R.string.category_event
        PassType.COUPON -> R.string.category_coupon
        PassType.LOYALTY -> R.string.category_storecard
        PassType.GENERIC -> R.string.category_generic
        PassType.VOUCHER -> R.string.categories_voucher
    }

/** the image shown for the pass type in the category indicator */
@get:DrawableRes
val PassType.topImageRes: Int
    get() = when (this) {
        PassType.BOARDING -> R.drawable.cat_bp
        PassType.PKBOARDING -> R.drawable.cat_bp
        PassType.EVENT -> R.drawable.cat_et
        PassType.COUPON -> R.drawable.cat_cp
        PassType.LOYALTY -> R.drawable.cat_sc
        PassType.VOUCHER -> R.drawable.cat_ps
        PassType.GENERIC -> R.drawable.cat_none
    }
