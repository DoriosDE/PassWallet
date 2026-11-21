package org.ligi.passandroid.ui.rendering

import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField

/** the first visible field of the group [hint] with a value */
internal fun Pass.frontField(hint: String): PassField? = frontFields(hint).firstOrNull()

/** the visible fields of the group [hint] with a value */
internal fun Pass.frontFields(hint: String): List<PassField> =
    fields.filter { !it.hide && it.hint == hint && !it.value.isNullOrBlank() }

/** the first visible field with a value whose key or label contains one of [needles] (accents ignored) */
internal fun Pass.frontFieldMatching(vararg needles: String): PassField? {
    val normalizedNeedles = needles.map { it.normalizedForMatching() }
    return fields.firstOrNull { field ->
        !field.hide && !field.value.isNullOrBlank() &&
            listOfNotNull(field.key, field.label)
                .joinToString(" ")
                .normalizedForMatching()
                .let { text -> normalizedNeedles.any { text.contains(it) } }
    }
}

/** "label: value", or only one of them when the other is missing - the key stands in for a missing label when asked */
internal fun joinLabelValue(field: PassField, keyAsLabelFallback: Boolean = false): String? {
    val label = field.label.cleanPassText() ?: field.key.takeIf { keyAsLabelFallback }.toDisplayKey()
    val value = field.value.cleanPassText()
    return when {
        label.isNullOrBlank() -> value
        value.isNullOrBlank() -> label
        else -> "$label: $value"
    }
}

/** "member_number" -> "Member Number" */
internal fun String?.toDisplayKey(): String? {
    val clean = cleanPassText() ?: return null
    return clean
        .replace('_', ' ')
        .replace('-', ' ')
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { char -> char.uppercase() } }
}

/** collapsed white space, a space after ")" before a time, no trailing " -" - null when nothing is left */
internal fun String?.cleanPassText(): String? {
    if (this == null) return null
    return trim()
        .replace(Regex("\\s+"), " ")
        .replace(Regex("\\)(?=\\d{1,2}:\\d{2})"), ") ")
        .replace(Regex("\\s+-\\s*$"), "")
        .ifBlank { null }
}

private fun String.normalizedForMatching(): String {
    return lowercase()
        .replace('á', 'a')
        .replace('é', 'e')
        .replace('í', 'i')
        .replace('ó', 'o')
        .replace('ú', 'u')
        .replace('ü', 'u')
        .replace('ñ', 'n')
        .replace(Regex("\\s+"), " ")
        .trim()
}
