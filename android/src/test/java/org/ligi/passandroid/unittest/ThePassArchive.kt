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
import org.mockito.Mockito.`when`

/**
 * The archive state of passes, tested in memory.
 */
class ThePassArchive {

    private val passes = listOf("a", "b", "c").associateWith { PassImpl(it) as Pass }

    private val passStore = mock(PassStore::class.java).apply {
        `when`(getPass(anyString())).thenAnswer { passes[it.arguments[0]] }
    }

    private var changes = 0

    private fun archive(vararg stored: Pair<String, String>) = PassArchive(mutableMapOf(*stored), passStore) { changes++ }

    @Test
    fun passesAreActiveUnlessArchived() {
        val tested = archive("a" to "active", "b" to "archived")

        assertThat(tested.stateOf(passes.getValue("a"))).isEqualTo(ArchiveState.ACTIVE)
        assertThat(tested.stateOf(passes.getValue("b"))).isEqualTo(ArchiveState.ARCHIVED)
        assertThat(tested.stateOf(passes.getValue("c"))).isEqualTo(ArchiveState.ACTIVE)
    }

    @Test
    fun listsPassesByState() {
        val tested = archive("a" to "active", "b" to "archived", "c" to "active")

        assertThat(tested.passesIn(ArchiveState.ACTIVE).map { it.id }).containsOnly("a", "c")
        assertThat(tested.passesIn(ArchiveState.ARCHIVED).map { it.id }).containsExactly("b")
    }

    @Test
    fun setsTheStateOfSeveralPassesAtOnce() {
        val tested = archive("a" to "active", "b" to "active", "c" to "active")

        tested.setState(listOf(passes.getValue("a"), passes.getValue("b")), ArchiveState.ARCHIVED)

        assertThat(tested.passesIn(ArchiveState.ARCHIVED).map { it.id }).containsOnly("a", "b")
        assertThat(tested.passesIn(ArchiveState.ACTIVE).map { it.id }).containsExactly("c")
    }

    @Test
    fun knowsWhichPassesItHoldsAndForgetsRemovedOnes() {
        val tested = archive("a" to "active", "b" to "archived")

        tested.removePass("a")

        assertThat(tested.knownPassIds).containsExactly("b")
    }

    @Test
    fun unknownValuesCountAsActive() {
        val tested = archive("a" to "", "b" to "something else", "c" to "archived")

        assertThat(tested.passesIn(ArchiveState.ACTIVE).map { it.id }).containsOnly("a", "b")
        assertThat(tested.isArchived(passes.getValue("c"))).isTrue()
    }

    @Test
    fun syncForgetsGonePassesAndAddsNewOnesAsActive() {
        val tested = archive("a" to "archived", "gone" to "active")

        tested.syncWith(listOf("a", "b"))

        assertThat(tested.knownPassIds).containsOnly("a", "b")
        assertThat(tested.isArchived(passes.getValue("a"))).isTrue()
        assertThat(tested.stateOf(passes.getValue("b"))).isEqualTo(ArchiveState.ACTIVE)
        assertThat(changes).isEqualTo(1)
    }

    @Test
    fun syncWithoutChangesDoesNotAnnounceAnything() {
        val tested = archive("a" to "archived", "b" to "active")

        tested.syncWith(listOf("a", "b"))

        assertThat(changes).isZero()
    }

    @Test
    fun setsTheStateOfASinglePass() {
        val tested = archive("a" to "active")

        tested.setState(passes.getValue("a"), ArchiveState.ARCHIVED)

        assertThat(tested.isArchived(passes.getValue("a"))).isTrue()
    }
}
