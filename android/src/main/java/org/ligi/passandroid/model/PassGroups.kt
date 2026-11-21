package org.ligi.passandroid.model

import org.ligi.passandroid.model.pass.Pass

/**
 * One row of the (grouped) pass list: either a single pass or a stack of passes that belong together.
 */
sealed class PassListEntry {
    data class Single(val pass: Pass) : PassListEntry()
    data class Group(val group: PassGrouping.InGroup, val passes: List<Pass>) : PassListEntry()
}

object PassGroups {

    /**
     * Collapses passes of the same group into one entry. The (already sorted) order is kept:
     * a group appears at the position of its first member, members keep their relative order.
     * Groups with only one member in the given list are shown as single passes.
     */
    fun group(passes: List<Pass>): List<PassListEntry> {
        val membersByGroup = passes.groupBy { it.grouping }

        val emittedGroups = HashSet<PassGrouping.InGroup>()
        val result = ArrayList<PassListEntry>()

        passes.forEach { pass ->
            val group = pass.grouping.group
            val members = group?.let { membersByGroup[it] }
            if (group == null || members == null || members.size < 2) {
                result.add(PassListEntry.Single(pass))
            } else if (emittedGroups.add(group)) {
                result.add(PassListEntry.Group(group, members))
            }
        }

        return result
    }

    /**
     * Puts [pass] into the same group as [target]. If [target] is not in a group yet a new group is created.
     * Returns the passes that were modified and need to be saved.
     */
    fun join(pass: Pass, target: Pass): List<Pass> {
        val existingGroup = target.grouping.group
        return if (existingGroup != null) {
            pass.grouping = existingGroup
            listOf(pass)
        } else {
            val newGroup = PassGrouping.newGroup()
            pass.grouping = newGroup
            target.grouping = newGroup
            listOf(pass, target)
        }
    }

    /**
     * Removes the given passes from their groups and marks them as ungrouped by the user
     * so neither the Passbook grouping nor the import matching groups them again.
     */
    fun ungroup(passes: List<Pass>): List<Pass> {
        passes.forEach { it.grouping = PassGrouping.UngroupedByUser }
        return passes
    }

    /**
     * An update replaces the stored pass: keep the grouping of the previous version unless the
     * updated pass brings its own Passbook group. Returns true when [updated] was changed.
     */
    fun keepGroupingAfterUpdate(previous: PassGrouping, updated: Pass): Boolean {
        if (updated.grouping.group != null || previous == PassGrouping.None) return false
        updated.grouping = previous
        return true
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
            if (pass.grouping != PassGrouping.None) return@forEach

            val matches = allCandidates.filter { belongTogether(pass, it) && it.grouping != PassGrouping.UngroupedByUser }
            val target = matches.firstOrNull { it.grouping.group != null } ?: matches.firstOrNull() ?: return@forEach
            changed.addAll(join(pass, target))
        }

        return changed.toList()
    }
}
