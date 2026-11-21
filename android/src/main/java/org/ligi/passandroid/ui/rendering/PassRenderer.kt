package org.ligi.passandroid.ui.rendering

import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.model.pass.PassVisualType

/**
 * How the texts of a kind of pass are shown: its title and line in the pass list, and the labels and values
 * of its fields in the pass view. [PassRenderers.forPass] picks the renderer for a pass.
 */
interface PassRenderer {
    fun listTitle(pass: Pass): String?
    fun listMeta(pass: Pass): String?

    fun detailLabel(field: PassField): String? = field.label.cleanPassText()
    fun detailValue(field: PassField): String? = field.value.cleanPassText()

    /** visible fields of the front groups - the type field the reader adds has no group, so it is never shown */
    fun showOnDetailFront(field: PassField): Boolean = !field.hide && field.hint in FRONT_FIELD_GROUPS
}

/** the pkpass field groups shown on the front of a pass */
internal val FRONT_FIELD_GROUPS = listOf("headerFields", "primaryFields", "secondaryFields", "auxiliaryFields")

object PassRenderers {
    fun forPass(pass: Pass): PassRenderer {
        if (pass.visualType == PassVisualType.CREDENTIAL) return CredentialPassRenderer

        return when (pass.type) {
            PassType.EVENT -> EventTicketRenderer
            PassType.BOARDING, PassType.PKBOARDING -> BoardingPassRenderer
            PassType.LOYALTY -> StoreCardRenderer
            else -> GenericPassRenderer
        }
    }
}
