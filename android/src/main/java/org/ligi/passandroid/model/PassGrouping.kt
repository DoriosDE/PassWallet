package org.ligi.passandroid.model

import java.util.UUID

/**
 * To which stack of passes a pass belongs.
 */
sealed class PassGrouping {

    /** the group the pass belongs to, null when it is in none */
    val group: InGroup? get() = this as? InGroup

    /** not part of a group - may be grouped automatically when a matching pass is imported */
    object None : PassGrouping()

    /** taken out of a group by the user - never grouped automatically again */
    object UngroupedByUser : PassGrouping()

    /** member of a group: all passes with the same [key] form one stack */
    data class InGroup(val key: String) : PassGrouping()

    companion object {
        /**
         * Passbook passes declare that they belong together via the same passTypeIdentifier and groupingIdentifier
         * (e.g. several tickets for the same event or the legs of a journey).
         */
        fun fromPassbook(passTypeIdentifier: String?, groupingIdentifier: String?): PassGrouping =
            if (groupingIdentifier.isNullOrBlank()) None else InGroup("pk:${passTypeIdentifier.orEmpty()}/$groupingIdentifier")

        /** a new group the user creates by grouping passes */
        fun newGroup() = InGroup("manual:" + UUID.randomUUID())
    }
}
