package org.ligi.passandroid.unittest

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.ligi.passandroid.model.ArchiveState
import org.ligi.passandroid.model.PassArchive
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.PassStoreProjection
import org.ligi.passandroid.model.PassStoreChange
import org.ligi.passandroid.model.PassSortOrder
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassType
import org.threeten.bp.ZonedDateTime
import java.io.File
import java.util.*

class ThePassSorting {

    private val pass1 = Pass("ID1")
    private val pass2 = Pass("ID2")
    private val pass3 = Pass("ID3")
    private val pass4 = Pass("ID4")
    private val pass5 = Pass("ID5")

    private lateinit var passList: List<Pass>

    @Before
    fun init() {
        pass1.calendarTimespan = Pass.TimeSpan(ZonedDateTime.now().minusHours(5))
        pass3.calendarTimespan = Pass.TimeSpan(ZonedDateTime.now().plusHours(1))
        pass2.calendarTimespan = Pass.TimeSpan(ZonedDateTime.now().plusHours(2))
        pass5.validTimespans = listOf(Pass.TimeSpan(ZonedDateTime.now().plusHours(3)))

        pass1.type = PassType.GENERIC
        pass2.type = PassType.EVENT
        pass3.type = PassType.GENERIC
        pass4.type = PassType.GENERIC
        pass5.type = PassType.EVENT

        passList = mutableListOf(pass1, pass2, pass3, pass4, pass5)
    }

    @Test
    fun testDESC() {
        Collections.sort(passList, PassSortOrder.DATE_DESC.comparator())

        assertThat(passList).containsExactly(pass4, pass5, pass2, pass3, pass1)
    }

    @Test
    fun testASC() {
        Collections.sort(passList, PassSortOrder.DATE_ASC.comparator())

        assertThat(passList).containsExactly(pass1, pass3, pass2, pass5, pass4)
    }

    @Test
    fun testDIFF() {
        Collections.sort(passList, PassSortOrder.DATE_DIFF.comparator())

        assertThat(passList).containsExactly(pass3, pass2, pass5, pass1, pass4)
    }

    @Test
    fun testTYPE() {
        Collections.sort(passList, PassSortOrder.TYPE.comparator())

        assertThat(passList).containsExactly(pass1, pass3, pass4, pass2, pass5)
    }

    @Test
    fun readsTheStoredSortOrder() {
        assertThat(PassSortOrder.entries.map { PassSortOrder.fromPreferenceValue(it.preferenceValue) })
            .containsExactlyElementsOf(PassSortOrder.entries)
    }

    @Test
    fun fallsBackToNewestFirstForUnknownStoredValues() {
        assertThat(PassSortOrder.fromPreferenceValue(42)).isEqualTo(PassSortOrder.DATE_DESC)
        assertThat(PassSortOrder.fromPreferenceValue(null)).isEqualTo(PassSortOrder.DATE_DESC)
    }

    @Test
    fun testProjectionRefreshCanApplyChangedSortOrder() {
        val passStore = ProjectionTestPassStore(passList.associateBy { it.id })
        val projection = PassStoreProjection(passStore, ArchiveState.ACTIVE, PassSortOrder.DATE_ASC)

        assertThat(projection.passList).containsExactly(pass1, pass3, pass2, pass5, pass4)

        projection.refresh(PassSortOrder.DATE_DESC)

        assertThat(projection.passList).containsExactly(pass4, pass5, pass2, pass3, pass1)
    }

    private class ProjectionTestPassStore(private val passesById: Map<String, Pass>) : PassStore {
        override val changes: SharedFlow<PassStoreChange> = MutableSharedFlow()
        override val archive = PassArchive(
            passesById.keys.associateWith { ArchiveState.ACTIVE.storedValue }.toMutableMap(),
            this,
        )

        override fun save(pass: Pass) = Unit
        override fun getPass(id: String): Pass? = passesById[id]
        override fun reload(id: String): Pass? = passesById[id]
        override fun reloadAll() = Unit
        override fun delete(id: String): Boolean = false
        override fun directoryOf(id: String): File = File(id)
        override fun syncArchiveWithStorage() = Unit
    }
}
