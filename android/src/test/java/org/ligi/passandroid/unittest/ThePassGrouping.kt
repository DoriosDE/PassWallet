package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.PassGroupFilter
import org.ligi.passandroid.model.PassGroups
import org.ligi.passandroid.model.PassGrouping
import org.ligi.passandroid.model.PassListEntry
import org.ligi.passandroid.model.pass.PassImpl

private fun pass(id: String, groupKey: String? = null) = PassImpl(id).apply {
    grouping = groupKey?.let { PassGrouping.InGroup(it) } ?: PassGrouping.None
}

class ThePassGrouping {

    @Test
    fun ungroupedPassesStaySingle() {
        val passes = listOf(pass("1"), pass("2").apply { grouping = PassGrouping.UngroupedByUser }, pass("3"))

        val result = PassGroups.group(passes)

        assertThat(result).containsExactly(
            PassListEntry.Single(passes[0]),
            PassListEntry.Single(passes[1]),
            PassListEntry.Single(passes[2]),
        )
    }

    @Test
    fun passesOfTheSameGroupAreCollapsedAtPositionOfFirstMember() {
        val a = pass("a")
        val g1 = pass("g1", "x")
        val b = pass("b")
        val g2 = pass("g2", "x")

        val result = PassGroups.group(listOf(a, g1, b, g2))

        assertThat(result).containsExactly(
            PassListEntry.Single(a),
            PassListEntry.Group(PassGrouping.InGroup("x"), listOf(g1, g2)),
            PassListEntry.Single(b),
        )
    }

    @Test
    fun groupWithSingleMemberIsShownAsSinglePass() {
        val lonely = pass("1", "x")

        assertThat(PassGroups.group(listOf(lonely))).containsExactly(PassListEntry.Single(lonely))
    }

    @Test
    fun passbookGroupingNeedsGroupingIdentifier() {
        assertThat(PassGrouping.fromPassbook("pass.com.example", null)).isEqualTo(PassGrouping.None)
        assertThat(PassGrouping.fromPassbook("pass.com.example", " ")).isEqualTo(PassGrouping.None)
        assertThat(PassGrouping.fromPassbook("pass.com.example", "event-42"))
            .isInstanceOf(PassGrouping.InGroup::class.java)
            .isEqualTo(PassGrouping.fromPassbook("pass.com.example", "event-42"))
        assertThat(PassGrouping.fromPassbook("pass.com.example", "event-42"))
            .isNotEqualTo(PassGrouping.fromPassbook("pass.com.other", "event-42"))
    }

    @Test
    fun joiningUngroupedTargetCreatesNewGroup() {
        val a = pass("a")
        val b = pass("b").apply { grouping = PassGrouping.UngroupedByUser }

        val changed = PassGroups.join(a, b)

        assertThat(changed).containsOnly(a, b).hasSize(2)
        assertThat(a.grouping).isInstanceOf(PassGrouping.InGroup::class.java).isEqualTo(b.grouping)
    }

    @Test
    fun joiningGroupedTargetReusesItsGroup() {
        val a = pass("a")
        val b = pass("b", "existing")

        val changed = PassGroups.join(a, b)

        assertThat(changed).containsExactly(a)
        assertThat(a.grouping).isEqualTo(PassGrouping.InGroup("existing"))
    }

    @Test
    fun ungroupMarksPassesAsExplicitlyUngrouped() {
        val a = pass("a", "x")

        PassGroups.ungroup(listOf(a))

        assertThat(a.grouping).isEqualTo(PassGrouping.UngroupedByUser)
    }

    @Test
    fun updatedPassKeepsTheGroupingOfThePreviousVersion() {
        val updated = pass("p")

        PassGroups.keepGroupingAfterUpdate(PassGrouping.InGroup("x"), updated)
        assertThat(updated.grouping).isEqualTo(PassGrouping.InGroup("x"))

        val updatedAfterUngroup = pass("q")
        PassGroups.keepGroupingAfterUpdate(PassGrouping.UngroupedByUser, updatedAfterUngroup)
        assertThat(updatedAfterUngroup.grouping).isEqualTo(PassGrouping.UngroupedByUser)
    }

    @Test
    fun updatedPassWithOwnPassbookGroupKeepsIt() {
        val updated = pass("p", "from-passbook")

        val changed = PassGroups.keepGroupingAfterUpdate(PassGrouping.InGroup("x"), updated)

        assertThat(changed).isFalse()
        assertThat(updated.grouping).isEqualTo(PassGrouping.InGroup("from-passbook"))
    }
}

class ThePassGroupFilter {


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
