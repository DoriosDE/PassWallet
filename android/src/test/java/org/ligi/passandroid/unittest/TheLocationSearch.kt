package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.location.LocationSearch
import org.ligi.passandroid.location.LocationSearchResult
import org.ligi.passandroid.location.parseNominatimResults
import java.io.IOException

class TheLocationSearch {

    @Test
    fun readsThePlacesOfAnAnswer() {
        val json = """
            [
              {"place_id": 1, "lat": "52.5170365", "lon": "13.3888599", "display_name": "Berlin, Deutschland", "type": "city"},
              {"place_id": 2, "lat": "48.1371079", "lon": "11.5753822", "display_name": "München, Bayern, Deutschland"}
            ]
        """.trimIndent()

        assertThat(parseNominatimResults(json)).containsExactly(
            LocationSearchResult("Berlin, Deutschland", 52.5170365, 13.3888599),
            LocationSearchResult("München, Bayern, Deutschland", 48.1371079, 11.5753822),
        )
    }

    @Test
    fun skipsPlacesWithoutNameOrValidCoordinates() {
        val json = """
            [
              {"lat": "52.5", "lon": "13.4"},
              {"lat": "52.5", "lon": "13.4", "display_name": "  "},
              {"lat": "north", "lon": "13.4", "display_name": "Nowhere"},
              {"lon": "13.4", "display_name": "No latitude"},
              {"lat": "52.5", "lon": "13.4", "display_name": "Berlin"}
            ]
        """.trimIndent()

        assertThat(parseNominatimResults(json)).containsExactly(LocationSearchResult("Berlin", 52.5, 13.4))
    }

    @Test
    fun readsAnEmptyAnswerAsNoPlaces() {
        assertThat(parseNominatimResults("[]")).isEmpty()
    }

    @Test(expected = IOException::class)
    fun rejectsAnAnswerThatIsNoListOfPlaces() {
        parseNominatimResults("""{"error": "Unable to geocode"}""")
    }

    @Test(expected = IOException::class)
    fun rejectsAnAnswerThatIsNoJson() {
        parseNominatimResults("<html>Too many requests</html>")
    }

    @Test
    fun encodesTheQueryInTheSearchUrl() {
        val url = LocationSearch().searchUrlFor("Café & Bar, Köln")

        assertThat(url.queryParameter("q")).isEqualTo("Café & Bar, Köln")
        assertThat(url.queryParameter("format")).isEqualTo("jsonv2")
        assertThat(url.queryParameter("limit")).isEqualTo("5")
        assertThat(url.toString()).startsWith(LocationSearch.NOMINATIM_SEARCH_URL + "?")
    }
}
