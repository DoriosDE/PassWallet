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

    /** marks a pass the user explicitly took out of a group - it is never grouped automatically again */
    const val USER_UNGROUPED = "none"

    /**
     * Passbook passes declare that they belong together via the same passTypeIdentifier and groupingIdentifier
     * (e.g. several tickets for the same event or the legs of a journey).
     */
    fun automaticGroupId(passTypeIdentifier: String?, groupingIdentifier: String?): String? {
        if (groupingIdentifier.isNullOrBlank()) return null
        return AUTOMATIC_PREFIX + passTypeIdentifier.orEmpty() + "/" + groupingIdentifier
    }

    fun newManualGroupId() = MANUAL_PREFIX + UUID.randomUUID().toString()

    fun isGrouped(pass: Pass) = !pass.groupId.isNullOrEmpty() && pass.groupId != USER_UNGROUPED

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
        val existingGroupId = target.groupId?.takeIf { isGrouped(target) }
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
     * Removes the given passes from their groups and marks them as explicitly ungrouped
     * so neither the Passbook grouping nor the import matching groups them again.
     */
    fun ungroup(passes: List<Pass>): List<Pass> {
        passes.forEach { it.groupId = USER_UNGROUPED }
        return passes
    }

    /**
     * Two passes belong together when they come from the same issuer (passTypeIdentifier), have the same type and
     * description and are for the same day - e.g. several tickets for one event or the boarding passes of one journey.
     * Passes without issuer or description (custom, image or PDF passes) never match.
     */
    fun belongTogether(a: Pass, b: Pass): Boolean {
        if (a.id == b.id) return false
        val issuer = a.passIdent?.takeIf { it.isNotBlank() } ?: return false
        val description = a.description?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        return issuer == b.passIdent &&
                a.type == b.type &&
                description.equals(b.description?.trim(), ignoreCase = true) &&
                a.calendarTimespan?.from?.toLocalDate() == b.calendarTimespan?.from?.toLocalDate()
    }

    /**
     * Automatically groups freshly imported [passes] with a matching pass from [candidates] (and with each other).
     * Passes that are already grouped or were ungrouped by the user are left alone.
     * Matches that are already in a group are preferred, so a new ticket joins an existing stack.
     *
     * Returns the passes that were changed and need to be saved.
     */
    fun autoGroup(passes: List<Pass>, candidates: List<Pass>): List<Pass> {
        val changed = LinkedHashSet<Pass>()
        val allCandidates = (candidates + passes).distinctBy { it.id }

        passes.forEach { pass ->
            if (isGrouped(pass) || pass.groupId == USER_UNGROUPED) return@forEach

            val matches = allCandidates.filter { belongTogether(pass, it) && it.groupId != USER_UNGROUPED }
            val target = matches.firstOrNull { isGrouped(it) } ?: matches.firstOrNull() ?: return@forEach
            changed.addAll(join(pass, target))
        }

        return changed.toList()
    }
}
