package org.ligi.passandroid.model

import org.ligi.passandroid.model.pass.Pass
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime

/**
 * The orders the pass list can be sorted in.
 *
 * @param preferenceValue how the order is stored in the sort preference
 */
enum class PassSortOrder(val preferenceValue: Int) {
    DATE_DESC(0),
    DATE_ASC(-1),
    TYPE(1),
    DATE_DIFF(2);

    /** a new comparator for this order - [DATE_DIFF] measures from the moment it is created */
    fun comparator(): Comparator<Pass> = when (this) {
        // reversed as a whole: passes without date come first
        DATE_DESC -> byDate.reversed()
        DATE_ASC -> byDate
        TYPE -> compareBy<Pass> { it.type }.then(byDate)
        DATE_DIFF -> byDistanceFrom(ZonedDateTime.now())
    }

    companion object {
        /** the order stored as [value] - unknown values fall back to [DATE_DESC] */
        fun fromPreferenceValue(value: Int?): PassSortOrder = entries.firstOrNull { it.preferenceValue == value } ?: DATE_DESC
    }
}

/** the date a pass is about: its calendar date, otherwise the start of its first validity period */
val Pass.sortDate: ZonedDateTime?
    get() = calendarTimespan?.from ?: validTimespans?.firstOrNull()?.from

/** earliest date first, passes without date last */
private val byDate: Comparator<Pass> = compareBy(nullsLast()) { it.sortDate }

/** closest to [now] first - in the past or in the future - passes without date last */
private fun byDistanceFrom(now: ZonedDateTime): Comparator<Pass> =
    compareBy(nullsLast()) { pass -> pass.sortDate?.let { Duration.between(now, it).abs() } }
