package org.ligi.passandroid.ui.rendering

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import org.ligi.kaxt.parseColor
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

/**
 * What a pass card in the list shows: logo (or logo text / title) on the left and the most important
 * header field (label + value) on the right, in the colors the pass defines - like a wallet card.
 */
class PassListAppearance(private val pass: Pass) {

    val backgroundColor: Int get() = pass.accentColor

    val foregroundColor: Int by lazy {
        pass.foregroundColorValue.parseColorOrNull() ?: readableForegroundFor(backgroundColor)
    }

    val labelColor: Int by lazy {
        pass.labelColorValue.parseColorOrNull() ?: ColorUtils.setAlphaComponent(foregroundColor, 200)
    }

    /** the logo text of the pass, if it defines one */
    val logoText: String? get() = pass.logoText?.trim()?.takeIf { it.isNotEmpty() }

    /** text on the left when there is no logo image: the title (the logo text belongs to the logo only) */
    val fallbackText: String by lazy {
        PassRenderers.forPass(pass).listTitle(pass)?.trim().orEmpty()
    }

    private val headerField: PassField? by lazy {
        pass.visibleField("headerFields") ?: pass.visibleField("secondaryFields")
    }

    val headerLabel: String? by lazy {
        headerField?.label?.cleaned()
    }

    val headerValue: String? by lazy {
        headerField?.value?.cleaned() ?: pass.calendarTimespan?.from?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }


    companion object {
        fun readableForegroundFor(backgroundColor: Int) =
            if (ColorUtils.calculateLuminance(backgroundColor) > 0.55) Color.BLACK else Color.WHITE

        private fun Pass.visibleField(hint: String) =
            fields.firstOrNull { !it.hide && it.hint == hint && !it.value.isNullOrBlank() }

        private fun String.cleaned() = trim().replace(Regex("\\s+"), " ").takeIf { it.isNotEmpty() }

        private fun String?.parseColorOrNull(): Int? {
            if (this.isNullOrBlank()) return null
            val parsed = parseColor(Color.TRANSPARENT)
            return parsed.takeIf { it != Color.TRANSPARENT }
        }
    }
}
