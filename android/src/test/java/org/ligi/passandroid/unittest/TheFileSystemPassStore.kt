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
}
