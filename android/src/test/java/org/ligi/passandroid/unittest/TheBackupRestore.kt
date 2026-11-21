package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.ligi.passandroid.backup.BackupArchive
import org.ligi.passandroid.backup.BackupRestore
import org.ligi.passandroid.model.ArchiveState
import org.ligi.passandroid.unittest.TestPassDirectory.Companion.pass
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class TheBackupRestore {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /** a backup with "both" (archived) and "restored" (active) */
    private fun backup(): ByteArray {
        val source = TestPassDirectory(temporaryFolder.newFolder())
        val store = source.newStore()
        val both = pass("both", "Backup version")
        val restored = pass("restored", "Restored")
        store.save(both)
        store.save(restored)
        store.archive.setState(both, ArchiveState.ARCHIVED)
        store.archive.setState(restored, ArchiveState.ACTIVE)

        val archive = ByteArrayOutputStream()
        BackupArchive.exportBackup(
            passesDir = source.settings.getPassesDir(),
            stateDir = source.settings.getStateDir(),
            preferences = emptyMap(),
            appVersionName = "test",
            appVersionCode = 1,
            outputStream = archive,
        )
        return archive.toByteArray()
    }

    @Test
    fun restoreCanRemoveThePassesThatAreNotInTheBackup() {
        val device = TestPassDirectory(temporaryFolder.newFolder())
        val store = device.newStore()
        val deviceOnly = pass("device-only", "Device only")
        store.save(deviceOnly)
        store.save(pass("both", "Device version"))
        store.archive.setState(deviceOnly, ArchiveState.ACTIVE)
        val backup = backup()

        BackupRestore.restore(
            openBackup = { ByteArrayInputStream(backup) },
            passStore = store,
            settings = device.settings,
            removePassesNotInBackup = true,
            restorePreferences = {},
        )

        assertThat(store.getPass("device-only")).isNull()
        assertThat(File(device.settings.getPassesDir(), "device-only")).doesNotExist()
        assertThat(store.getPass("both")!!.description).isEqualTo("Backup version")
        assertThat(store.archive.passesIn(ArchiveState.ARCHIVED).map { it.id }).containsExactly("both")
        assertThat(store.archive.passesIn(ArchiveState.ACTIVE).map { it.id }).containsExactly("restored")
    }

    @Test
    fun countsThePassesThatWouldBeRemoved() {
        val device = TestPassDirectory(temporaryFolder.newFolder())
        val store = device.newStore()
        store.save(pass("device-only", "Device only"))
        store.save(pass("both", "Device version"))
        store.syncArchiveWithStorage()
        val backup = backup()

        assertThat(BackupRestore.passesNotInBackup(store) { ByteArrayInputStream(backup) }).containsExactly("device-only")
    }

    @Test
    fun fileThatIsNotABackupIsRejectedAndNothingIsRemoved() {
        val device = TestPassDirectory(temporaryFolder.newFolder())
        val store = device.newStore()
        store.save(pass("device-only", "Device only"))
        store.syncArchiveWithStorage()
        val notABackup = "just some text".toByteArray()

        try {
            BackupRestore.restore(
                openBackup = { ByteArrayInputStream(notABackup) },
                passStore = store,
                settings = device.settings,
                removePassesNotInBackup = true,
                restorePreferences = {},
            )
            fail("Expected a file that is not a backup to be rejected")
        } catch (expected: BackupRestore.NotABackupException) {
            // expected
        }

        assertThat(store.getPass("device-only")).isNotNull
    }
}
