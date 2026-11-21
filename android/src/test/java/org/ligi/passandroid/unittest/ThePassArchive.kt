package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.ArchiveState
import org.ligi.passandroid.model.PassArchive
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassImpl
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * The archive state of passes, tested in memory.
 */
class ThePassArchive {

    private val passes = listOf("a", "b", "c").associateWith { PassImpl(it) as Pass }

    private val passStore = mock(PassStore::class.java).apply {
        `when`(getPassbookForId(anyString())).thenAnswer { passes[it.arguments[0]] }
    }

    private fun archive(vararg stored: Pair<String, String>) = PassArchive(mutableMapOf(*stored), passStore)

    @Test
    fun passesAreActiveUnlessArchived() {
        val tested = archive("a" to "new", "b" to "archive")

        assertThat(tested.stateOf(passes.getValue("a"))).isEqualTo(ArchiveState.ACTIVE)
        assertThat(tested.stateOf(passes.getValue("b"))).isEqualTo(ArchiveState.ARCHIVED)
        assertThat(tested.stateOf(passes.getValue("c"))).isEqualTo(ArchiveState.ACTIVE)
    }

    @Test
    fun listsPassesByState() {
        val tested = archive("a" to "new", "b" to "archive", "c" to "new")

        assertThat(tested.passesIn(ArchiveState.ACTIVE).map { it.id }).containsOnly("a", "c")
        assertThat(tested.passesIn(ArchiveState.ARCHIVED).map { it.id }).containsExactly("b")
    }

    @Test
    fun setsTheStateOfSeveralPassesAtOnce() {
        val tested = archive("a" to "new", "b" to "new", "c" to "new")

        tested.setState(listOf(passes.getValue("a"), passes.getValue("b")), ArchiveState.ARCHIVED)

        assertThat(tested.passesIn(ArchiveState.ARCHIVED).map { it.id }).containsOnly("a", "b")
        assertThat(tested.passesIn(ArchiveState.ACTIVE).map { it.id }).containsExactly("c")
    }

    @Test
    fun knowsWhichPassesItHoldsAndForgetsRemovedOnes() {
        val tested = archive("a" to "new", "b" to "archive")

        tested.removePass("a")

        assertThat(tested.knownPassIds).containsExactly("b")
    }

    @Test
    fun leftoverValuesFromOldVersionsCountAsActive() {
        val tested = archive("a" to "trash", "b" to "Concerts", "c" to "archive")

        assertThat(tested.passesIn(ArchiveState.ACTIVE).map { it.id }).containsOnly("a", "b")
        assertThat(tested.isArchived(passes.getValue("c"))).isTrue()
    }

    @Test
    fun syncForgetsGonePassesAndAddsNewOnesAsActive() {
        val tested = archive("a" to "archive", "gone" to "new")

        tested.syncWith(listOf("a", "b"))

        assertThat(tested.knownPassIds).containsOnly("a", "b")
        assertThat(tested.isArchived(passes.getValue("a"))).isTrue()
        assertThat(tested.stateOf(passes.getValue("b"))).isEqualTo(ArchiveState.ACTIVE)
        verify(passStore, times(1)).notifyChange()
    }

    @Test
    fun syncWithoutChangesDoesNotAnnounceAnything() {
        val tested = archive("a" to "archive", "b" to "new")

        tested.syncWith(listOf("a", "b"))

        verify(passStore, never()).notifyChange()
    }

    @Test
    fun setsTheStateOfASinglePass() {
        val tested = archive("a" to "new")

        tested.setState(passes.getValue("a"), ArchiveState.ARCHIVED)

        assertThat(tested.isArchived(passes.getValue("a"))).isTrue()
    }
}
