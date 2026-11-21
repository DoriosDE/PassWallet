package org.ligi.passandroid.model

import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.Pass
import java.util.*

/**
 * Restricts a projection to parts of a list depending on pass groups.
 */
sealed class PassGroupFilter {
    /** every pass of the list */
    object All : PassGroupFilter()

    /** only the members of one group */
    data class OnlyGroup(val groupId: String) : PassGroupFilter()

    /** only passes that are not stacked in a group (with at least two members in this list) */
    object OnlyUngrouped : PassGroupFilter()

    fun apply(passes: List<Pass>): List<Pass> = when (this) {
        All -> passes
        is OnlyGroup -> passes.filter { it.groupId == groupId }
        OnlyUngrouped -> PassGroups.group(passes).filterIsInstance<PassListEntry.Single>().map { it.pass }
    }

    companion object {
        /**
         * The filter used when looking at [pass] in the detail view: swiping only switches between the passes of its group,
         * or - for passes outside of groups - between the other ungrouped passes.
         */
        fun forPass(pass: Pass, listPasses: List<Pass>): PassGroupFilter {
            val group = PassGroups.group(listPasses)
                .filterIsInstance<PassListEntry.Group>()
                .firstOrNull { it.groupId == pass.groupId }
            return if (group != null) OnlyGroup(group.groupId) else OnlyUngrouped
        }
    }
}

class PassStoreProjection(
    private val passStore: PassStore,
    private val state: ArchiveState,
    private var passSortOrder: PassSortOrder? = null,
    var groupFilter: PassGroupFilter = PassGroupFilter.All,
) {

    var passList: List<Pass> = ArrayList()
        private set

    init {
        refresh()
    }

    fun refresh(passSortOrder: PassSortOrder? = this.passSortOrder) {
        this.passSortOrder = passSortOrder
        val newPassList = passStore.archive.passesIn(state)
        if (passSortOrder != null) {
            Collections.sort(newPassList, passSortOrder.toComparator())
        }

        passList = groupFilter.apply(newPassList)
    }
}
