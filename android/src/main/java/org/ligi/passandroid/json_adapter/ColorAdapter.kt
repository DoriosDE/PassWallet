package org.ligi.passandroid.json_adapter

import androidx.core.graphics.toColorInt
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.ligi.passandroid.model.pass.PassImpl

class ColorAdapter {
    @ToJson
    internal fun toJson(@PassImpl.HexColor rgb: Int) = String.format("#%06x", rgb)

    @FromJson
    @PassImpl.HexColor
    internal fun fromJson(rgb: String) = parseHexColor(rgb) ?: rgb.toColorInt()

    /**
     * Colors are written as "#rrggbb" or "#aarrggbb" - parsed without Android so stored passes can be read anywhere.
     * Returns null for any other notation.
     */
    private fun parseHexColor(value: String): Int? {
        val hex = value.removePrefix("#")
        if (!value.startsWith("#") || hex.any { Character.digit(it, 16) < 0 }) return null
        return when (hex.length) {
            6 -> (0xFF000000L or hex.toLong(16)).toInt()
            8 -> hex.toLong(16).toInt()
            else -> null
        }
    }
}
