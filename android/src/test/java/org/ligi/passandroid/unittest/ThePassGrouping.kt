package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.PassGroupFilter
import org.ligi.passandroid.model.PassGroups
import org.ligi.passandroid.model.PassListEntry
import org.ligi.passandroid.model.pass.PassImpl

class ThePassGrouping {

    private fun pass(id: String, groupId: String? = null) = PassImpl(id).apply { this.groupId = groupId }

    @Test
    fun ungroupedPassesStaySingle() {
        val passes = listOf(pass("1"), pass("2", ""), pass("3"))

        val result = PassGroups.group(passes)

        assertThat(result).containsExactly(
            PassListEntry.Single(passes[0]),
            PassListEntry.Single(passes[1]),
            PassListEntry.Single(passes[2]),
        )
    }

    @Test
    fun passesWithSameGroupIdAreCollapsedAtPositionOfFirstMember() {
        val a = pass("a")
        val g1 = pass("g1", "x")
        val b = pass("b")
        val g2 = pass("g2", "x")

        val result = PassGroups.group(listOf(a, g1, b, g2))

        assertThat(result).containsExactly(
            PassListEntry.Single(a),
            PassListEntry.Group("x", listOf(g1, g2)),
            PassListEntry.Single(b),
        )
    }

    @Test
    fun groupWithSingleMemberIsShownAsSinglePass() {
        val lonely = pass("1", "x")

        assertThat(PassGroups.group(listOf(lonely))).containsExactly(PassListEntry.Single(lonely))
    }

    @Test
    fun automaticGroupIdNeedsGroupingIdentifier() {
        assertThat(PassGroups.automaticGroupId("pass.com.example", null)).isNull()
        assertThat(PassGroups.automaticGroupId("pass.com.example", " ")).isNull()
        assertThat(PassGroups.automaticGroupId("pass.com.example", "event-42"))
            .isEqualTo(PassGroups.automaticGroupId("pass.com.example", "event-42"))
        assertThat(PassGroups.automaticGroupId("pass.com.example", "event-42"))
            .isNotEqualTo(PassGroups.automaticGroupId("pass.com.other", "event-42"))
    }

    @Test
    fun joiningUngroupedTargetCreatesNewGroup() {
        val a = pass("a")
        val b = pass("b", "")

        val changed = PassGroups.join(a, b)

        assertThat(changed).containsOnly(a, b).hasSize(2)
        assertThat(a.groupId).isNotEmpty().isEqualTo(b.groupId)
    }

    @Test
    fun joiningGroupedTargetReusesItsGroup() {
        val a = pass("a")
        val b = pass("b", "existing")

        val changed = PassGroups.join(a, b)

        assertThat(changed).containsExactly(a)
        assertThat(a.groupId).isEqualTo("existing")
    }

    @Test
    fun ungroupMarksPassesAsExplicitlyUngrouped() {
        val a = pass("a", "x")

        PassGroups.ungroup(listOf(a))

        assertThat(a.groupId).isEqualTo(PassGroups.USER_UNGROUPED)
        assertThat(PassGroups.isGrouped(a)).isFalse()
    }
}

class ThePassGroupFilter {

    private fun pass(id: String, groupId: String? = null) = PassImpl(id).apply { this.groupId = groupId }

    private val single = pass("single")
    private val lonely = pass("lonely", "y")
    private val member1 = pass("m1", "x")
    private val member2 = pass("m2", "x")
    private val topic = listOf(single, member1, lonely, member2)

    @Test
    fun groupedPassOnlySwipesThroughItsGroup() {
        val filter = PassGroupFilter.forPass(member2, topic)

        assertThat(filter.apply(topic)).containsExactly(member1, member2)
    }

    @Test
    fun ungroupedPassSkipsStackedGroups() {
        val filter = PassGroupFilter.forPass(single, topic)

        assertThat(filter.apply(topic)).containsExactly(single, lonely)
    }

    @Test
    fun passInGroupOfOneIsTreatedAsUngrouped() {
        val filter = PassGroupFilter.forPass(lonely, topic)

        assertThat(filter.apply(topic)).containsExactly(single, lonely)
    }
}
