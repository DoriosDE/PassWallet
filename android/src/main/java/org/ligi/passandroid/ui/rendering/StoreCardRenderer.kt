package org.ligi.passandroid.ui.rendering

import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField

/** store and loyalty cards: a primary field without value shows its label as value */
object StoreCardRenderer : PassRenderer {
    override fun listTitle(pass: Pass): String? {
        return pass.frontField("primaryFields")?.let { field ->
            field.value.cleanPassText() ?: field.label.cleanPassText()
        } ?: pass.description
    }

    override fun listMeta(pass: Pass): String? {
        return pass.frontField("secondaryFields")?.let { joinLabelValue(it) }
            ?: pass.frontField("headerFields")?.value.cleanPassText()
            ?: pass.creator
    }

    override fun detailLabel(field: PassField): String? {
        if (field.isPrimaryWithoutValue()) return null
        return field.label.cleanPassText()
    }

    override fun detailValue(field: PassField): String? {
        if (field.isPrimaryWithoutValue()) return field.label.cleanPassText()
        return field.value.cleanPassText()
    }

    private fun PassField.isPrimaryWithoutValue() = hint == "primaryFields" && value.cleanPassText() == null
}
