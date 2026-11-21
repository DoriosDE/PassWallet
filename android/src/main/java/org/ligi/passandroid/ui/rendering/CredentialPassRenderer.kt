package org.ligi.passandroid.ui.rendering

import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField

/** membership cards and other credentials: holder name and member / id number, found by their labels */
object CredentialPassRenderer : PassRenderer {
    override fun listTitle(pass: Pass): String? {
        return pass.frontFieldMatching("nombre", "name", "titular", "holder")?.value.cleanPassText()
            ?: pass.frontField("secondaryFields")?.value.cleanPassText()
            ?: pass.description
    }

    override fun listMeta(pass: Pass): String? {
        val credentialId = pass.frontFieldMatching("colegiado", "colegiada", "member", "membership")
            ?: pass.frontFieldMatching("nif", "dni", "d.n.i", "id")
        return credentialId?.let { joinLabelValue(it, keyAsLabelFallback = true) }
            ?: pass.creator
            ?: pass.description
    }

    override fun detailLabel(field: PassField): String? {
        return field.label.cleanPassText() ?: field.key.toDisplayKey()
    }
}
