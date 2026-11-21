package org.ligi.passandroid.model.pass

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PassField(var key: String?, var label: String?, var value: String?, var hide: Boolean, var hint:String? = null) {

    fun toHtmlSnippet(): String {
        val result = StringBuilder()

        label?.let { result.append("<b>$label</b> ") }
        value?.let { result.append(value) }

        if (label != null || value != null) {
            result.append("<br/>")
        }
        return "$result"
    }

}