package org.ligi.passandroid.ui.rendering

import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField

/** boarding passes: the route as title, times, gate and seat as list line */
object BoardingPassRenderer : PassRenderer {
    override fun listTitle(pass: Pass): String? {
        val route = pass.frontFields("primaryFields")
            .mapNotNull { it.value.cleanPassText() }
            .take(2)
            .joinToString(" → ")
        return route.ifBlank {
            pass.frontField("primaryFields")?.value.cleanPassText()
                ?: pass.description
        }
    }

    override fun listMeta(pass: Pass): String? {
        val travelMeta = pass.fields
            .asSequence()
            .filter { !it.hide && it.hint in listOf("headerFields", "secondaryFields", "auxiliaryFields") }
            .filter { it.looksLikeTravelTimeOrCode() }
            .mapNotNull { joinLabelValue(it) }
            .take(2)
            .joinToString(" · ")

        return travelMeta.ifBlank {
            pass.frontField("headerFields")?.let { joinLabelValue(it) }
                ?: pass.creator
                ?: pass.description
        }
    }
}

private val TRAVEL_WORDS = listOf(
    "date", "time", "valid", "departure", "arrival", "boarding", "gate", "seat", "code",
    "fecha", "hora", "salida", "llegada", "embarque", "puerta", "asiento", "codigo", "código",
    "datum", "zeit", "gültig", "gueltig",
)

private fun PassField.looksLikeTravelTimeOrCode(): Boolean {
    val text = listOfNotNull(key, label).joinToString(" ").lowercase()
    return TRAVEL_WORDS.any { text.contains(it) }
}
