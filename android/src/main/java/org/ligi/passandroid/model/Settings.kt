package org.ligi.passandroid.model

import java.io.File

interface Settings {

    fun getSortOrder(): PassSortOrder

    fun doTraceDroidEmailSend(): Boolean

    fun getPassesDir(): File

    fun getStateDir(): File

    fun isAutomaticLightEnabled(): Boolean

    fun isGroupingEnabled(): Boolean
}
