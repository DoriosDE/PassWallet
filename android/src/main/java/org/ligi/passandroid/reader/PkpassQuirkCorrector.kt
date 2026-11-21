package org.ligi.passandroid.reader

import org.ligi.passandroid.model.pass.PassField
import org.ligi.passandroid.model.pass.Pass
import org.threeten.bp.DateTimeException
import org.threeten.bp.ZonedDateTime

class PkpassQuirkCorrector {

    fun correctQuirks(pass: Pass) {
        // Vendor specific fixes
        careForTUIFlight(pass)
        careForAirBerlin(pass)
        careForWestbahn(pass)
        careForAirCanada(pass)
        careForUSAirways(pass)
        careForVirginAustralia(pass)
        careForCathayPacific(pass)
        careForSWISS(pass)
        careForRenfe(pass)

        //general fixes
        tryToFindDate(pass)
    }

    private fun tryToFindDate(pass: Pass) {

        if (pass.calendarTimespan == null) {
            val foundDate = pass.fields.filter { "date" == it.key }.map {
                try {
                    ZonedDateTime.parse(it.value)
                } catch (e: DateTimeException) {
                    null
                }
            }.firstOrNull { it != null }

            if (foundDate != null) {
                pass.calendarTimespan = Pass.TimeSpan(from = foundDate)
            }
        }

    }

    private fun getPassFieldForKey(pass: Pass, key: String): PassField? {
        return pass.fields.firstOrNull { it.key != null && it.key == key }
    }

    private fun getPassFieldThatMatchesLabel(pass: Pass, matcher: String): PassField? {
        return pass.fields.firstOrNull {
            val label = it.label
            label != null && label.matches(matcher.toRegex())
        }
    }

    private fun careForRenfe(pass: Pass) {
        if (pass.creator == null || pass.creator != "RENFE OPERADORA") {
            return
        }

        val optionalDepart = getPassFieldForKey(pass, "boardingTime")
        val optionalArrive = getPassFieldForKey(pass, "destino")

        if (optionalDepart != null && optionalArrive != null) {
            pass.description = optionalDepart.label + " -> " + optionalArrive.label
        }
    }

    private fun careForSWISS(pass: Pass) {
        if (pass.creator == null || pass.creator != "SWISS") {
            return
        }

        val optionalDepart = getPassFieldForKey(pass, "depart")
        val optionalArrive = getPassFieldForKey(pass, "destination")

        if (optionalDepart != null && optionalArrive != null) {
            pass.description = optionalDepart.value + " -> " + optionalArrive.value
        }
    }

    private fun careForCathayPacific(pass: Pass) {
        if (pass.creator == null || pass.creator != "Cathay Pacific") {

            return
        }


        val optionalDepart = getPassFieldForKey(pass, "departure")
        val optionalArrive = getPassFieldForKey(pass, "arrival")

        if (optionalDepart != null && optionalArrive != null) {
            pass.description = optionalDepart.label + " -> " + optionalArrive.label
        }
    }

    private fun careForVirginAustralia(pass: Pass) {
        // also good identifier could be  "passTypeIdentifier": "pass.com.virginaustralia.boardingpass
        if (pass.creator == null || pass.creator != "Virgin Australia") {
            return
        }


        val optionalDepart = getPassFieldForKey(pass, "origin")
        val optionalArrive = getPassFieldForKey(pass, "destination")

        if (optionalDepart != null && optionalArrive != null) {
            pass.description = optionalDepart.label + " -> " + optionalArrive.label
        }
    }

    private fun careForAirCanada(pass: Pass) {
        if (pass.creator == null || pass.creator != "Air Canada") {
            return
        }


        val optionalDepart = getPassFieldForKey(pass, "depart")
        val optionalArrive = getPassFieldForKey(pass, "arrive")

        if (optionalDepart != null && optionalArrive != null) {
            pass.description = optionalDepart.label + " -> " + optionalArrive.label
        }
    }

    private fun careForUSAirways(pass: Pass) {
        if (pass.creator == null || pass.creator != "US Airways") {
            return
        }


        val optionalDepart = getPassFieldForKey(pass, "depart")
        val optionalArrive = getPassFieldForKey(pass, "destination")

        if (optionalDepart != null && optionalArrive != null) {
            pass.description = optionalDepart.label + " -> " + optionalArrive.label
        }
    }

    private fun careForWestbahn(pass: Pass) {
        if (pass.calendarTimespan != null || (pass.creator ?: "") != "WESTbahn") {
            return
        }


        val originField = getPassFieldForKey(pass, "from")
        val destinationField = getPassFieldForKey(pass, "to")

        var description = "WESTbahn"

        if (originField != null) {
            val value = originField.value
            if (value != null) {
                description = value
            }
        }

        if (destinationField != null) {
            description += "->" + destinationField.value!!
        }

        pass.description = description
    }

    private fun careForTUIFlight(pass: Pass) {
        if (pass.description != "TUIfly pass") {
            return
        }


        val originField = getPassFieldForKey(pass, "Origin")
        val destinationField = getPassFieldForKey(pass, "Des")
        val seatField = getPassFieldForKey(pass, "SeatNumber")

        if (originField != null && destinationField != null) {
            var description = originField.value + "->" + destinationField.value
            if (seatField != null) {
                description += " @" + seatField.value!!
            }
            pass.description = description
        }
    }


    private fun careForAirBerlin(pass: Pass) {
        if (pass.description != "boardcard") {
            return
        }

        val flightRegex = "\\b\\w{1,3}\\d{3,4}\\b"

        var flightField = getPassFieldThatMatchesLabel(pass, flightRegex)

        if (flightField == null) {
            flightField = getPassFieldThatMatchesLabel(pass, flightRegex)
        }
        val seatField = getPassFieldForKey(pass, "seat")
        val boardingGroupField = getPassFieldForKey(pass, "boardingGroup")

        if (flightField != null && seatField != null && boardingGroupField != null) {
            var description = flightField.label + " " + flightField.value
            description += " | " + seatField.label + " " + seatField.value
            description += " | " + boardingGroupField.label + " " + boardingGroupField.value
            pass.description = description
        } // otherwise fallback to default - better save than sorry
    }
}
