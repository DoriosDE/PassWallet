package org.ligi.passandroid.model

import com.chibatching.kotpref.KotprefModel

object State : KotprefModel() {

    var lastSelectedPassUUID: String by stringPref()

}
