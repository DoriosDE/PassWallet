package org.ligi.passandroid.json_adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.ligi.passandroid.model.PassGrouping

/**
 * Stores a [PassGrouping] as "none", "ungrouped" or "group:<key>".
 */
class PassGroupingAdapter {

    @ToJson
    internal fun toJson(grouping: PassGrouping) = when (grouping) {
        PassGrouping.None -> NONE
        PassGrouping.UngroupedByUser -> UNGROUPED
        is PassGrouping.InGroup -> GROUP_PREFIX + grouping.key
    }

    @FromJson
    internal fun fromJson(value: String): PassGrouping = when {
        value == UNGROUPED -> PassGrouping.UngroupedByUser
        value.startsWith(GROUP_PREFIX) && value.length > GROUP_PREFIX.length -> PassGrouping.InGroup(value.removePrefix(GROUP_PREFIX))
        else -> PassGrouping.None
    }

    private companion object {
        const val NONE = "none"
        const val UNGROUPED = "ungrouped"
        const val GROUP_PREFIX = "group:"
    }
}
