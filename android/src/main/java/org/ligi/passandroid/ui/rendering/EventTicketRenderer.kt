package org.ligi.passandroid.ui.rendering

import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField

/** event tickets: venue and date in the list, compact room and seat values */
object EventTicketRenderer : PassRenderer {
    override fun listTitle(pass: Pass): String? {
        return pass.frontField("primaryFields")?.value.cleanPassText()
            ?: pass.description
    }

    override fun listMeta(pass: Pass): String? {
        val venue = pass.frontField("primaryFields")?.label.cleanPassText()
        val date = pass.frontField("headerFields")?.value.cleanPassText()
        val combined = listOfNotNull(venue, date).joinToString(" · ")
        return combined.ifBlank { pass.description.orEmpty() }.ifBlank { null }
    }

    override fun detailValue(field: PassField): String? {
        val label = field.label.cleanPassText().orEmpty().lowercase()
        val value = field.value.cleanPassText()
        return when {
            value == null -> null
            label.contains("sala") && value.endsWith("-") -> value.dropLast(1).trim().ifBlank { value }
            label.contains("fila") && label.contains("asiento") -> compactSeatList(value)
            else -> value
        }
    }
}

/** "7-12, 7-13, 7-14" (row-seat) -> "Fila 7 · Asientos 12-14" when all seats are in one row */
private fun compactSeatList(value: String): String {
    val matches = Regex("(\\d+)\\s*-\\s*(\\d+)").findAll(value).map { match ->
        match.groupValues[1].toInt() to match.groupValues[2].toInt()
    }.toList()

    if (matches.size < 2 || matches.size != value.split(',').size) return value

    val rows = matches.map { it.first }.distinct()
    if (rows.size != 1) return value

    val seats = matches.map { it.second }.sorted()
    val compactSeats = if (seats.zipWithNext().all { it.second == it.first + 1 }) {
        "${seats.first()}-${seats.last()}"
    } else {
        seats.joinToString(", ")
    }

    return "Fila ${rows.first()} · Asientos $compactSeats"
}
