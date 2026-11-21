package org.ligi.passandroid.unittest

import android.content.Context
import com.squareup.moshi.Moshi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.json_adapter.ColorAdapter
import org.ligi.passandroid.json_adapter.ZonedTimeAdapter
import org.ligi.passandroid.model.AndroidFileSystemPassStore
import org.ligi.passandroid.model.PassArchive
import org.ligi.passandroid.model.Settings
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.PassImpl
import org.mockito.Mockito.mock
import java.io.File

/**
 * The pass store tested through its interface on a real (temporary) directory.
 */
class TheFileSystemPassStore {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val root by lazy { temporaryFolder.newFolder() }

    private val settings by lazy {
        object : Settings {
            override fun getSortOrder() = PassSortOrder.DATE_DESC
            override fun doTraceDroidEmailSend() = false
            override fun getPassesDir() = File(root, "passes")
            override fun getStateDir() = File(root, "state")
            override fun isAutomaticLightEnabled() = false
            override fun isGroupingEnabled() = true
        }
    }

    private val moshi = Moshi.Builder().add(ZonedTimeAdapter()).add(ColorAdapter()).build()

    /** a fresh store on the same directory - like the app after a restart */
    private fun newStore() = AndroidFileSystemPassStore(mock(Context::class.java), settings, moshi, mock(Tracker::class.java)) { "en" }

    private fun pass(id: String, description: String) = PassImpl(id).apply {
        this.description = description
        groupId = "manual:group"
    }

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
