package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.reader.PkpassQuirkCorrector
import org.ligi.passandroid.model.pass.PassField
import org.ligi.passandroid.model.pass.Pass
import org.threeten.bp.ZonedDateTime
import java.util.*

private const val DATE_PROBE = "2016-07-06T21:00:00-04:00"

class ThePkpassQuirkCorrector {
    private val tested = PkpassQuirkCorrector()

    @Test
    fun testThatItDoesNothingWithNoField() {
        val pass = Pass(UUID.randomUUID().toString())

        tested.correctQuirks(pass)

        assertThat(pass.calendarTimespan).isNull()
    }


    @Test
    fun testThatDateIsExtracted() {
        val pass = Pass(UUID.randomUUID().toString())

        pass.fields = mutableListOf(PassField("date", "foo", DATE_PROBE, false))

        tested.correctQuirks(pass)

        assertThat(pass.calendarTimespan!!.from).isEqualTo(ZonedDateTime.parse(DATE_PROBE))
    }

    @Test
    fun testThatInvalidDateIsIgnored() {
        val pass = Pass(UUID.randomUUID().toString())

        pass.fields = mutableListOf(PassField("date", "foo", "invalid", false))

        tested.correctQuirks(pass)

        assertThat(pass.calendarTimespan).isNull()
    }

    @Test
    fun testThatDateIsExtractedAfterWrongDatesBefore() {
        val pass = Pass(UUID.randomUUID().toString())

        pass.fields = mutableListOf(PassField("date", "foo", "invalid", false), PassField("date", "foo", DATE_PROBE, false))

        tested.correctQuirks(pass)

        assertThat(pass.calendarTimespan!!.from).isEqualTo(ZonedDateTime.parse(DATE_PROBE))
    }

}
