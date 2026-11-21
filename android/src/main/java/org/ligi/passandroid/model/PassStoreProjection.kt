package org.ligi.passandroid.model

import org.ligi.passandroid.model.pass.Pass

/**
 * Restricts a projection to parts of a list depending on pass groups.
 */
sealed class PassGroupFilter {
    /** every pass of the list */
    object All : PassGroupFilter()

    /** only the members of one group */
    data class OnlyGroup(val group: PassGrouping.InGroup) : PassGroupFilter()

    /** only passes that are not stacked in a group (with at least two members in this list) */
    object OnlyUngrouped : PassGroupFilter()

    fun apply(passes: List<Pass>): List<Pass> = when (this) {
        All -> passes
        is OnlyGroup -> passes.filter { it.grouping == group }
        OnlyUngrouped -> PassGroups.group(passes).filterIsInstance<PassListEntry.Single>().map { it.pass }
    }

    companion object {
        /**
         * The filter used when looking at [pass] in the detail view: swiping only switches between the passes of its group,
         * or - for passes outside of groups - between the other ungrouped passes.
         */
        fun forPass(pass: Pass, listPasses: List<Pass>): PassGroupFilter {
            val groupEntry = PassGroups.group(listPasses)
                .filterIsInstance<PassListEntry.Group>()
                .firstOrNull { it.group == pass.grouping }
            return if (groupEntry != null) OnlyGroup(groupEntry.group) else OnlyUngrouped
        }
    }
}

class PassStoreProjection(
    private val passStore: PassStore,
    private val state: ArchiveState,
    private var passSortOrder: PassSortOrder? = null,
    var groupFilter: PassGroupFilter = PassGroupFilter.All,
) {

    var passList: List<Pass> = emptyList()
        private set

    init {
        refresh()
    }

    fun refresh(passSortOrder: PassSortOrder? = this.passSortOrder) {
        this.passSortOrder = passSortOrder
        val passes = passStore.archive.passesIn(state)
        val sorted = if (passSortOrder != null) passes.sortedWith(passSortOrder.comparator()) else passes
        passList = groupFilter.apply(sorted)
    }
}
