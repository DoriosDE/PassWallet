package org.ligi.passandroid.ui.rendering

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.model.pass.PassVisualType

class ThePassRenderers {

    private fun field(hint: String?, label: String?, value: String?, key: String? = null, hide: Boolean = false) =
        PassField(key, label, value, hide, hint)

    private fun pass(type: PassType, vararg fields: PassField) = Pass("id").apply {
        this.type = type
        description = "Description"
        creator = "Creator"
        this.fields = fields.toMutableList()
    }

    /** the type field the pkpass reader adds - never on the front */
    private val typeField = PassField("", "Typ", "Ereignis", false)

    @Test
    fun picksTheRendererForTheKindOfPass() {
        assertThat(PassRenderers.forPass(pass(PassType.EVENT))).isSameAs(EventTicketRenderer)
        assertThat(PassRenderers.forPass(pass(PassType.PKBOARDING))).isSameAs(BoardingPassRenderer)
        assertThat(PassRenderers.forPass(pass(PassType.LOYALTY))).isSameAs(StoreCardRenderer)
        assertThat(PassRenderers.forPass(pass(PassType.COUPON))).isSameAs(GenericPassRenderer)
        assertThat(PassRenderers.forPass(pass(PassType.EVENT).apply { visualType = PassVisualType.CREDENTIAL }))
            .isSameAs(CredentialPassRenderer)
    }

    @Test
    fun genericPassesShowThePrimaryValueAndHeader() {
        val pass = pass(PassType.GENERIC, field("primaryFields", "Name", "  Gym   Card "), field("headerFields", "Level", "Gold"))

        assertThat(GenericPassRenderer.listTitle(pass)).isEqualTo("Gym Card")
        assertThat(GenericPassRenderer.listMeta(pass)).isEqualTo("Gold")
        assertThat(GenericPassRenderer.listTitle(pass(PassType.GENERIC))).isEqualTo("Description")
    }

    @Test
    fun boardingPassesShowTheRouteAndTravelData() {
        val pass = pass(
            PassType.PKBOARDING,
            field("primaryFields", "From", "MAD"),
            field("primaryFields", "To", "BCN"),
            field("headerFields", "Gate", "B12"),
            field("auxiliaryFields", "Seat", "14C"),
            field("secondaryFields", "Passenger", "Jane"),
        )

        assertThat(BoardingPassRenderer.listTitle(pass)).isEqualTo("MAD → BCN")
        assertThat(BoardingPassRenderer.listMeta(pass)).isEqualTo("Gate: B12 · Seat: 14C")
    }

    @Test
    fun eventTicketsCompactSeatsInOneRow() {
        val seats = field("secondaryFields", "Fila-Asiento", "7-12, 7-13, 7-14")

        assertThat(EventTicketRenderer.detailValue(seats)).isEqualTo("Fila 7 · Asientos 12-14")
        assertThat(EventTicketRenderer.detailValue(field("headerFields", "Sala", "Sala 3 -"))).isEqualTo("Sala 3")
        assertThat(EventTicketRenderer.listMeta(pass(PassType.EVENT, field("primaryFields", "Cine", "Film"), field("headerFields", "Date", "Fri"))))
            .isEqualTo("Cine · Fri")
    }

    @Test
    fun storeCardsShowTheLabelWhenThePrimaryFieldHasNoValue() {
        val primary = field("primaryFields", "Gold Member", " ")

        assertThat(StoreCardRenderer.detailLabel(primary)).isNull()
        assertThat(StoreCardRenderer.detailValue(primary)).isEqualTo("Gold Member")
        assertThat(StoreCardRenderer.listMeta(pass(PassType.LOYALTY))).isEqualTo("Creator")
    }

    @Test
    fun credentialsFindHolderAndIdByTheirLabels() {
        val pass = pass(
            PassType.GENERIC,
            field("secondaryFields", "Nombre", "Ana García"),
            field("auxiliaryFields", null, "12345", key = "member_number"),
        )

        assertThat(CredentialPassRenderer.listTitle(pass)).isEqualTo("Ana García")
        assertThat(CredentialPassRenderer.listMeta(pass)).isEqualTo("Member Number: 12345")
    }

    @Test
    fun onlyVisibleFieldsOfTheFrontGroupsAreOnTheFront() {
        val renderers = listOf(GenericPassRenderer, StoreCardRenderer, CredentialPassRenderer, EventTicketRenderer, BoardingPassRenderer)
        renderers.forEach { renderer ->
            assertThat(renderer.showOnDetailFront(field("primaryFields", "A", "B"))).isTrue()
            assertThat(renderer.showOnDetailFront(field("backFields", "A", "B", hide = true))).isFalse()
            assertThat(renderer.showOnDetailFront(typeField)).isFalse()
        }
    }
}
