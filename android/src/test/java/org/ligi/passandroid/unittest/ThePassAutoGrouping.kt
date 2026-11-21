package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.PassGrouping
import org.ligi.passandroid.model.PassGroups
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.model.pass.PassType
import org.threeten.bp.ZonedDateTime

class ThePassAutoGrouping {

    private val concertDay = ZonedDateTime.parse("2026-12-05T20:00:00+01:00")

    private fun ticket(
        id: String,
        issuer: String? = "pass.com.tickets",
        description: String = "Concert ticket",
        date: ZonedDateTime? = concertDay,
        type: PassType = PassType.EVENT,
        grouping: PassGrouping = PassGrouping.None,
    ) = PassImpl(id).apply {
        passIdent = issuer
        this.description = description
        calendarTimespan = date?.let { PassImpl.TimeSpan(from = it) }
        this.type = type
        this.grouping = grouping
    }

    @Test
    fun newTicketForSameEventIsGroupedWithExistingPass() {
        val existing = ticket("existing")
        val imported = ticket("imported", date = concertDay.plusMinutes(30))

        val changed = PassGroups.autoGroup(listOf(imported), listOf(existing))

        assertThat(changed).containsOnly(imported, existing).hasSize(2)
        assertThat(imported.grouping).isInstanceOf(PassGrouping.InGroup::class.java).isEqualTo(existing.grouping)
    }

    @Test
    fun newTicketJoinsExistingGroup() {
        val grouped = ticket("grouped", grouping = PassGrouping.InGroup("stack"))
        val imported = ticket("imported")

        assertThat(PassGroups.autoGroup(listOf(imported), listOf(ticket("other", description = "Other"), grouped)))
            .containsExactly(imported)
        assertThat(imported.grouping).isEqualTo(PassGrouping.InGroup("stack"))
    }

    @Test
    fun differentIssuerDayDescriptionOrTypeDoNotMatch() {
        val existing = ticket("existing")

        listOf(
            ticket("a", issuer = "pass.com.other"),
            ticket("b", date = concertDay.plusDays(1)),
            ticket("c", description = "Parking"),
            ticket("d", type = PassType.COUPON),
            ticket("e", issuer = null),
            ticket("f", description = " "),
        ).forEach { candidate ->
            assertThat(PassGroups.autoGroup(listOf(candidate), listOf(existing))).describedAs(candidate.id).isEmpty()
        }
    }

    @Test
    fun passesUngroupedByUserAreLeftAlone() {
        val ungroupedByUser = ticket("existing", grouping = PassGrouping.UngroupedByUser)
        val imported = ticket("imported")

        assertThat(PassGroups.autoGroup(listOf(imported), listOf(ungroupedByUser))).isEmpty()

        val importedAgain = ticket("again", grouping = PassGrouping.UngroupedByUser)
        assertThat(PassGroups.autoGroup(listOf(importedAgain), listOf(ticket("x")))).isEmpty()
    }

    @Test
    fun passesImportedTogetherAreGroupedWithEachOther() {
        val first = ticket("1")
        val second = ticket("2")

        PassGroups.autoGroup(listOf(first, second), emptyList())

        assertThat(first.grouping).isInstanceOf(PassGrouping.InGroup::class.java).isEqualTo(second.grouping)
    }
}
