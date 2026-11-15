package org.ligi.passandroid.model

import org.ligi.passandroid.model.pass.Pass
import java.util.UUID

/**
 * One row of the (grouped) pass list: either a single pass or a stack of passes that belong together.
 */
sealed class PassListEntry {
    data class Single(val pass: Pass) : PassListEntry()
    data class Group(val groupId: String, val passes: List<Pass>) : PassListEntry()
}

object PassGroups {

    private const val AUTOMATIC_PREFIX = "pk:"
    private const val MANUAL_PREFIX = "manual:"

    /**
     * Passbook passes declare that they belong together via the same passTypeIdentifier and groupingIdentifier
     * (e.g. several tickets for the same event or the legs of a journey).
     */
    fun automaticGroupId(passTypeIdentifier: String?, groupingIdentifier: String?): String? {
        if (groupingIdentifier.isNullOrBlank()) return null
        return AUTOMATIC_PREFIX + passTypeIdentifier.orEmpty() + "/" + groupingIdentifier
    }

    fun newManualGroupId() = MANUAL_PREFIX + UUID.randomUUID().toString()

    fun isGrouped(pass: Pass) = !pass.groupId.isNullOrEmpty()

    /**
     * Collapses passes with the same group id into one entry. The (already sorted) order is kept:
     * a group appears at the position of its first member, members keep their relative order.
     * Groups with only one member in the given list are shown as single passes.
     */
    fun group(passes: List<Pass>): List<PassListEntry> {
        val membersByGroup = passes
            .filter { isGrouped(it) }
            .groupBy { it.groupId!! }

        val emittedGroups = HashSet<String>()
        val result = ArrayList<PassListEntry>()

        passes.forEach { pass ->
            val groupId = pass.groupId
            val members = groupId?.let { membersByGroup[it] }
            if (groupId == null || members == null || members.size < 2) {
                result.add(PassListEntry.Single(pass))
            } else if (emittedGroups.add(groupId)) {
                result.add(PassListEntry.Group(groupId, members))
            }
        }

        return result
    }

    /**
     * Puts [pass] into the same group as [target]. If [target] is not grouped yet a new group is created.
     * Returns the passes that were modified and need to be saved.
     */
    fun join(pass: Pass, target: Pass): List<Pass> {
        val existingGroupId = target.groupId?.takeIf { it.isNotEmpty() }
        return if (existingGroupId != null) {
            pass.groupId = existingGroupId
            listOf(pass)
        } else {
            val newGroupId = newManualGroupId()
            pass.groupId = newGroupId
            target.groupId = newGroupId
            listOf(pass, target)
        }
    }

    /**
     * Removes the given passes from their groups. An empty group id marks them as explicitly ungrouped
     * so the automatic Passbook grouping is not applied again.
     */
    fun ungroup(passes: List<Pass>): List<Pass> {
        passes.forEach { it.groupId = "" }
        return passes
    }
}
