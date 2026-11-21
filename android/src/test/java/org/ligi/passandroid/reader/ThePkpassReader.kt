package org.ligi.passandroid.reader

import android.content.Context
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.ligi.passandroid.model.PassGrouping
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

class ThePkpassReader {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context = mock(Context::class.java).apply {
        `when`(getString(anyInt())).thenReturn("Type")
    }

    private fun passDirectory(passJson: String, vararg files: Pair<String, String>) = temporaryFolder.newFolder("pass-id").apply {
        File(this, "pass.json").writeText(passJson)
        files.forEach { (name, content) -> File(this, name).apply { parentFile?.mkdirs() }.writeText(content) }
    }

    private val eventTicket = """
        {
          "description": "Concert",
          "organizationName": "Example Hall",
          "serialNumber": "123",
          "passTypeIdentifier": "pass.org.example.event",
          "webServiceURL": "https://example.org/passes",
          "authenticationToken": "secret",
          "relevantDate": "2026-12-24T20:00+01:00",
          "barcodes": [{"format": "PKBarcodeFormatQR", "message": "ticket-123", "altText": "123"}],
          "locations": [{"latitude": 52.5, "longitude": 13.4, "relevantText": "Entrance"}],
          "eventTicket": {
            "primaryFields": [{"key": "event", "label": "Event", "value": "Concert"}],
            "backFields": [{"key": "terms", "label": "Terms", "value": "No refunds"}]
          }
        }
    """.trimIndent()

    @Test
    fun readsTheDataOfAnEventTicket() {
        val pass = PkpassReader.read(passDirectory(eventTicket), "en", context)!!

        assertThat(pass.id).isEqualTo("pass-id")
        assertThat(pass.type).isEqualTo(PassType.EVENT)
        assertThat(pass.description).isEqualTo("Concert")
        assertThat(pass.creator).isEqualTo("Example Hall")
        assertThat(pass.serial).isEqualTo("123")
        assertThat(pass.passIdent).isEqualTo("pass.org.example.event")
        assertThat(pass.webServiceURL).isEqualTo("https://example.org/passes")
        assertThat(pass.authToken).isEqualTo("secret")
        assertThat(pass.calendarTimespan?.from?.year).isEqualTo(2026)
        assertThat(pass.barCode?.format).isEqualTo(PassBarCodeFormat.QR_CODE)
        assertThat(pass.barCode?.message).isEqualTo("ticket-123")
        assertThat(pass.barCode?.alternativeText).isEqualTo("123")
        assertThat(pass.locations.map { Triple(it.lat, it.lon, it.name) }).containsExactly(Triple(52.5, 13.4, "Entrance"))
        assertThat(pass.grouping).isEqualTo(PassGrouping.fromPassbook("pass.org.example.event", null))
    }

    @Test
    fun readsFrontAndHiddenBackFieldsPlusTheType() {
        val pass = PkpassReader.read(passDirectory(eventTicket), "en", context)!!

        assertThat(pass.fields.map { Triple(it.label, it.value, it.hide) }).containsExactly(
            Triple("Event", "Concert", false),
            Triple("Terms", "No refunds", true),
            Triple("Type", "Type", false),
        )
        assertThat(pass.fields.first().hint).isEqualTo("primaryFields")
    }

    @Test
    fun translatesTextsIntoTheLanguageOfTheDevice() {
        val directory = passDirectory(
            eventTicket.replace("\"Concert\"", "\"EVENT_NAME\""),
            "de.lproj/pass.strings" to "\"EVENT_NAME\" = \"Konzert\";",
        )

        val pass = PkpassReader.read(directory, "de", context)!!

        assertThat(pass.description).isEqualTo("Konzert")
        assertThat(pass.fields.first().value).isEqualTo("Konzert")
    }

    @Test
    fun badDatesAndBarcodesLeaveTheRestOfThePass() {
        val directory = passDirectory(
            """{"description": "Odd", "relevantDate": "2013-12-25T00:00-57:00", "barcode": {"format": "PKBarcodeFormatQR"}}"""
        )

        val pass = PkpassReader.read(directory, "en", context)!!

        assertThat(pass.description).isEqualTo("Odd")
        assertThat(pass.calendarTimespan).isNull()
        assertThat(pass.barCode).isNull()
    }

    @Test
    fun withSeveralStylesTheLastKnownOneIsUsed() {
        val pass = PkpassReader.read(passDirectory("""{"coupon": {}, "storeCard": {}}"""), "en", context)!!

        assertThat(pass.type).isEqualTo(PassType.LOYALTY)
    }

    @Test
    fun takesTheBestPictureVersion() {
        val directory = passDirectory(
            eventTicket,
            "icon.png" to "icon",
            "icon@2x.png" to "icon@2x",
            "logo.png" to "logo",
            "en.lproj/strip.png" to "strip-en",
            "strip@2x.png" to "strip@2x",
        )

        PkpassReader.read(directory, "de", context)

        assertThat(File(directory, "icon.png").readText()).isEqualTo("icon@2x")
        assertThat(File(directory, "logo.png").readText()).isEqualTo("logo")
        assertThat(File(directory, "strip.png").readText()).isEqualTo("strip-en")
        assertThat(File(directory, "footer.png")).doesNotExist()
    }

    @Test
    fun withoutPassJsonThereIsNoPass() {
        val directory = temporaryFolder.newFolder("empty")

        assertThat(PkpassReader.read(directory, "en", context)).isNull()
    }
}
