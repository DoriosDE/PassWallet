package org.ligi.passandroid.ui.rendering

import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField

/** generic passes, coupons and vouchers */
object GenericPassRenderer : PassRenderer {
    override fun listTitle(pass: Pass): String? {
        return pass.frontField("primaryFields")?.value.cleanPassText()
            ?: pass.description
    }

    override fun listMeta(pass: Pass): String? {
        return pass.frontField("headerFields")?.value.cleanPassText()
            ?: pass.frontField("secondaryFields")?.let { joinLabelValue(it) }
    }

    /** every visible field with a group - passes made in the app have fields without */
    override fun showOnDetailFront(field: PassField): Boolean = !field.hide && field.hint != null
}
