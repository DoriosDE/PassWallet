package org.ligi.passandroid.backup

import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.Settings
import java.io.InputStream

/**
 * Restores a PassWallet backup into the app's storage.
 */
object BackupRestore {

    /** the chosen file is not a PassWallet backup - nothing was changed */
    class NotABackupException : IllegalArgumentException("Not a PassWallet backup")

    /**
     * Ids of the passes on the device that are not contained in the backup -
     * the ones a restore with removePassesNotInBackup deletes.
     *
     * @throws NotABackupException when the file is not a PassWallet backup
     */
    fun passesNotInBackup(passStore: PassStore, openBackup: () -> InputStream): Set<String> {
        val backupIds = readBackupPassIds(openBackup)
        return passStore.classifier.topicByIdMap.keys.filterNot { it in backupIds }.toSet()
    }

    /** checks the file first, so a wrong file can never lead to removed passes */
    private fun readBackupPassIds(openBackup: () -> InputStream): Set<String> {
        if (!openBackup().use { BackupArchive.looksLikePassWalletBackup(it) }) {
            throw NotABackupException()
        }
        return openBackup().use { BackupArchive.passIds(it) }
    }

    /**
     * Copies the passes, archive state and preferences of the backup over the current ones.
     * Passes from the backup get their backed up archive state, others keep theirs - unless
     * [removePassesNotInBackup] is set: then every pass not contained in the backup is deleted.
     *
     * @param openBackup opens the backup file - it is read several times
     * @throws NotABackupException when the file is not a PassWallet backup (nothing is changed then)
     */
    fun restore(
        openBackup: () -> InputStream,
        passStore: PassStore,
        settings: Settings,
        removePassesNotInBackup: Boolean,
        restorePreferences: (Map<String, Any>) -> Unit,
    ) {
        val backupIds = readBackupPassIds(openBackup)

        openBackup().use { input ->
            BackupArchive.importBackup(
                inputStream = input,
                passesDir = settings.getPassesDir(),
                stateDir = settings.getStateDir(),
                restorePreferences = restorePreferences,
            )
        }

        passStore.reloadAll()

        if (removePassesNotInBackup) {
            passStore.classifier.topicByIdMap.keys
                .filterNot { it in backupIds }
                .forEach { passStore.deletePassWithId(it) }
        }
    }
}
