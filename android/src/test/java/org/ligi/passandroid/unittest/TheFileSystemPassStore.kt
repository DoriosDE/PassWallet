package org.ligi.passandroid.unittest

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.ligi.passandroid.model.ArchiveState
import org.ligi.passandroid.model.PassGrouping
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.PassVisualType
import org.ligi.passandroid.model.applyUpdate
import java.io.File

/**
 * The pass store tested through its interface on a real (temporary) directory.
 */
class TheFileSystemPassStore {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val directory by lazy { TestPassDirectory(temporaryFolder.newFolder()) }
    private val settings get() = directory.settings

    private fun newStore() = directory.newStore()

    private fun pass(id: String, description: String) = TestPassDirectory.pass(id, description)

    /** how many change notifications [store] sends while [action] runs - several quick ones may be merged into one */
    private fun countNotifications(store: PassStore, action: () -> Unit): Int = runBlocking {
        var count = 0
        val collector = launch(start = CoroutineStart.UNDISPATCHED) { store.updateChannel.collect { count++ } }
        action()
        yield()
        collector.cancel()
        count
    }

    @Test
    fun savingAPassNotifiesListeners() {
        val store = newStore()

        assertThat(countNotifications(store) { store.save(pass("p1", "Concert")) }).isEqualTo(1)
    }

    @Test
    fun changingTheArchiveStateNotifiesListeners() {
        val store = newStore()
        val saved = pass("p1", "Concert")
        store.save(saved)

        assertThat(countNotifications(store) { store.archive.setState(saved, ArchiveState.ARCHIVED) }).isEqualTo(1)
    }

    @Test
    fun deletingAPassNotifiesListeners() {
        val store = newStore()
        store.save(pass("p1", "Concert"))

        assertThat(countNotifications(store) { store.deletePassWithId("p1") }).isEqualTo(1)
    }

    @Test
    fun readingAPassThatIsBroughtUpToDateNotifiesNobody() {
        newStore().save(pass("p1", "Concert").apply { visualType = PassVisualType.CREDENTIAL })
        val store = newStore()

        val notifications = countNotifications(store) {
            assertThat(store.getPassbookForId("p1")!!.visualType).isEqualTo(PassVisualType.DEFAULT)
        }

        assertThat(notifications).isZero()
    }

    @Test
    fun savedPassSurvivesRestartWithGroupAndArchiveState() {
        val store = newStore()
        val saved = pass("p1", "Concert")
        store.save(saved)
        store.archive.setState(saved, ArchiveState.ARCHIVED)

        val restarted = newStore()
        val loaded = restarted.getPassbookForId("p1")

        assertThat(loaded).isNotNull
        assertThat(loaded!!.description).isEqualTo("Concert")
        assertThat(loaded.grouping).isEqualTo(PassGrouping.InGroup("group"))
        assertThat(restarted.archive.isArchived(loaded)).isTrue()
    }

    @Test
    fun deletedPassIsGoneFromStoreAndLists() {
        val store = newStore()
        val kept = pass("kept", "Kept")
        val deleted = pass("deleted", "Deleted")
        store.save(kept)
        store.save(deleted)
        store.archive.setState(kept, ArchiveState.ARCHIVED)
        store.archive.setState(deleted, ArchiveState.ARCHIVED)

        assertThat(store.deletePassWithId("deleted")).isTrue()

        assertThat(store.getPassbookForId("deleted")).isNull()
        assertThat(store.archive.passesIn(ArchiveState.ARCHIVED).map { it.id }).containsExactly("kept")
        val restarted = newStore()
        assertThat(restarted.getPassbookForId("deleted")).isNull()
        assertThat(restarted.archive.passesIn(ArchiveState.ARCHIVED).map { it.id }).containsExactly("kept")
    }

    @Test
    fun reloadReadsPassAgainAfterItsFilesWereReplaced() {
        val store = newStore()
        store.save(pass("p1", "Old version"))
        assertThat(store.getPassbookForId("p1")!!.description).isEqualTo("Old version")

        // files replaced behind the store's back - as a pass update does
        newStore().save(pass("p1", "New version"))

        assertThat(store.reload("p1")!!.description).isEqualTo("New version")
        assertThat(store.getPassbookForId("p1")!!.description).isEqualTo("New version")
    }

    @Test
    fun reloadOfRemovedPassReturnsNull() {
        val store = newStore()
        store.save(pass("p1", "Concert"))
        File(settings.getPassesDir(), "p1").deleteRecursively()

        assertThat(store.reload("p1")).isNull()
        assertThat(store.getPassbookForId("p1")).isNull()
    }

    @Test
    fun reloadAllMergesRestoredPassesAndArchiveStateWithDevicePasses() {
        val store = newStore()
        val deviceOnly = pass("device-only", "Device only")
        val both = pass("both", "Device version")
        store.save(deviceOnly)
        store.save(both)
        store.archive.setState(deviceOnly, ArchiveState.ARCHIVED)
        store.archive.setState(both, ArchiveState.ACTIVE)

        // a backup import copies its passes and its archive state over the existing files
        File(settings.getPassesDir(), "both").deleteRecursively()
        val backup = newStore()
        backup.save(pass("both", "Backup version"))
        backup.save(pass("restored", "Restored"))
        File(settings.getStateDir(), "archive_state.json")
            .writeText("""{"both":"${ArchiveState.ARCHIVED.storedValue}","restored":"${ArchiveState.ARCHIVED.storedValue}"}""")

        store.reloadAll()

        assertThat(store.getPassbookForId("both")!!.description).isEqualTo("Backup version")
        assertThat(store.archive.passesIn(ArchiveState.ARCHIVED).map { it.id })
            .containsOnly("device-only", "both", "restored")
        assertThat(store.archive.passesIn(ArchiveState.ACTIVE)).isEmpty()
    }

    @Test
    fun reloadAllWithBrokenArchiveStateKeepsCurrentState() {
        val store = newStore()
        val archived = pass("archived", "Archived")
        store.save(archived)
        store.archive.setState(archived, ArchiveState.ARCHIVED)
        File(settings.getStateDir(), "archive_state.json").writeText("[1, 2, 3]")

        store.reloadAll()

        assertThat(store.archive.passesIn(ArchiveState.ARCHIVED).map { it.id }).containsExactly("archived")
    }

    @Test
    fun readsTheStoredArchiveState() {
        val store = newStore()
        store.save(pass("archived", "Archived"))
        store.save(pass("active", "Active"))
        settings.getStateDir().mkdirs()
        File(settings.getStateDir(), "archive_state.json").writeText("""{"archived":"archived","active":"active"}""")

        val restarted = newStore()
        restarted.syncArchiveWithStorage()

        assertThat(restarted.archive.passesIn(ArchiveState.ARCHIVED).map { it.id }).containsExactly("archived")
        assertThat(restarted.archive.passesIn(ArchiveState.ACTIVE).map { it.id }).containsExactly("active")
    }

    @Test
    fun ungroupedByUserAndNoGroupSurviveRestart() {
        val store = newStore()
        store.save(pass("ungrouped", "Ungrouped").apply { grouping = PassGrouping.UngroupedByUser })
        store.save(pass("single", "Single").apply { grouping = PassGrouping.None })

        val restarted = newStore()

        assertThat(restarted.getPassbookForId("ungrouped")!!.grouping).isEqualTo(PassGrouping.UngroupedByUser)
        assertThat(restarted.getPassbookForId("single")!!.grouping).isEqualTo(PassGrouping.None)
    }

    @Test
    fun applyingAnUpdateWithANewIdReplacesTheOldPassAndKeepsItsGroup() {
        val store = newStore()
        val old = pass("old-id", "Old").apply { grouping = PassGrouping.InGroup("stack") }
        store.save(old)
        store.archive.setState(old, ArchiveState.ARCHIVED)
        // the update was imported under a new id
        newStore().save(pass("new-id", "New").apply { grouping = PassGrouping.None })

        val updated = store.applyUpdate(previous = old, updatedPassId = "new-id")

        assertThat(updated!!.description).isEqualTo("New")
        assertThat(updated.grouping).isEqualTo(PassGrouping.InGroup("stack"))
        assertThat(store.getPassbookForId("old-id")).isNull()
        assertThat(newStore().getPassbookForId("new-id")!!.grouping).isEqualTo(PassGrouping.InGroup("stack"))
    }
}
