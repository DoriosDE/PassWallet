package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.ligi.passandroid.model.PassArchive
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

    @Test
    fun savedPassSurvivesRestartWithGroupAndArchiveState() {
        val store = newStore()
        val saved = pass("p1", "Concert")
        store.save(saved)
        store.classifier.moveToTopic(saved, PassArchive.ARCHIVE)

        val restarted = newStore()
        val loaded = restarted.getPassbookForId("p1")

        assertThat(loaded).isNotNull
        assertThat(loaded!!.description).isEqualTo("Concert")
        assertThat(loaded.groupId).isEqualTo("manual:group")
        assertThat(PassArchive.isArchived(restarted.classifier, loaded)).isTrue()
    }

    @Test
    fun deletedPassIsGoneFromStoreAndLists() {
        val store = newStore()
        val kept = pass("kept", "Kept")
        val deleted = pass("deleted", "Deleted")
        store.save(kept)
        store.save(deleted)
        store.classifier.moveToTopic(kept, PassArchive.ARCHIVE)
        store.classifier.moveToTopic(deleted, PassArchive.ARCHIVE)

        assertThat(store.deletePassWithId("deleted")).isTrue()

        assertThat(store.getPassbookForId("deleted")).isNull()
        assertThat(store.classifier.getPassListByTopic(PassArchive.ARCHIVE).map { it.id }).containsExactly("kept")
        val restarted = newStore()
        assertThat(restarted.getPassbookForId("deleted")).isNull()
        assertThat(restarted.classifier.getPassListByTopic(PassArchive.ARCHIVE).map { it.id }).containsExactly("kept")
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
        store.classifier.moveToTopic(deviceOnly, PassArchive.ARCHIVE)
        store.classifier.moveToTopic(both, PassArchive.ACTIVE)

        // a backup import copies its passes and its archive state over the existing files
        File(settings.getPassesDir(), "both").deleteRecursively()
        val backup = newStore()
        backup.save(pass("both", "Backup version"))
        backup.save(pass("restored", "Restored"))
        File(settings.getStateDir(), "classifier_state.json")
            .writeText("""{"both":"${PassArchive.ARCHIVE}","restored":"${PassArchive.ARCHIVE}"}""")

        store.reloadAll()

        assertThat(store.getPassbookForId("both")!!.description).isEqualTo("Backup version")
        assertThat(store.classifier.getPassListByTopic(PassArchive.ARCHIVE).map { it.id })
            .containsOnly("device-only", "both", "restored")
        assertThat(store.classifier.getPassListByTopic(PassArchive.ACTIVE)).isEmpty()
    }

    @Test
    fun reloadAllWithBrokenArchiveStateKeepsCurrentState() {
        val store = newStore()
        val archived = pass("archived", "Archived")
        store.save(archived)
        store.classifier.moveToTopic(archived, PassArchive.ARCHIVE)
        File(settings.getStateDir(), "classifier_state.json").writeText("[1, 2, 3]")

        store.reloadAll()

        assertThat(store.classifier.getPassListByTopic(PassArchive.ARCHIVE).map { it.id }).containsExactly("archived")
    }
}
