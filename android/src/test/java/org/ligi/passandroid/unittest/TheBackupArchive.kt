package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.ligi.passandroid.backup.BackupArchive
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class TheBackupArchive {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun exportIncludesManifestPassesStateAndPreferences() {
        val root = temporaryFolder.newFolder()
        val passesDir = File(root, "passes")
        val passDir = File(passesDir, "pass-1")
        passDir.mkdirs()
        File(passDir, "main.json").writeText("{\"id\":\"pass-1\"}")
        val stateDir = File(root, "state")
        stateDir.mkdirs()
        File(stateDir, "archive_state.json").writeText("{\"pass-1\":\"tickets\"}")
        val output = ByteArrayOutputStream()

        BackupArchive.exportBackup(
            passesDir = passesDir,
            stateDir = stateDir,
            preferences = mapOf("sort" to "0", "autolight" to true),
            appVersionName = "26.4.29",
            appVersionCode = 202604290,
            outputStream = output,
        )

        val entries = zipEntries(output.toByteArray())
        assertThat(entries.keys).contains(
            "manifest.json",
            "passes/pass-1/main.json",
            "state/archive_state.json",
            "preferences.json",
        )
        assertThat(BackupArchive.readSummary(ByteArrayInputStream(output.toByteArray()))?.app).isEqualTo("PassWallet")
        assertThat(BackupArchive.decodePreferences(entries.getValue("preferences.json")))
            .isEqualTo(mapOf("sort" to "0", "autolight" to true))
    }

    @Test
    fun preferencesKeepTheirTypes() {
        val preferences = mapOf(
            "text" to "a \"quoted\"\nvalue",
            "flag" to false,
            "count" to 42,
            "big" to 9_000_000_000L,
            "ratio" to 0.75f,
        )

        assertThat(BackupArchive.decodePreferences(BackupArchive.encodePreferences(preferences))).isEqualTo(preferences)
    }

    @Test
    fun preferencesOfOtherTypesAreLeftOut() {
        val encoded = BackupArchive.encodePreferences(mapOf("set" to setOf("a"), "none" to null, "kept" to "yes"))

        assertThat(BackupArchive.decodePreferences(encoded)).isEqualTo(mapOf("kept" to "yes"))
    }

    @Test
    fun brokenOrUnknownPreferencesAreSkipped() {
        val json = """[
            {"key":"kept","type":"int","value":"7"},
            {"key":"unknownType","type":"date","value":"2026-01-01"},
            {"key":"brokenNumber","type":"long","value":"many"}
        ]"""

        assertThat(BackupArchive.decodePreferences(json)).isEqualTo(mapOf("kept" to 7))
        assertThat(BackupArchive.decodePreferences("not json")).isEmpty()
    }

    @Test
    fun importRestoresPassesStateAndPreferences() {
        val sourceRoot = temporaryFolder.newFolder()
        val sourcePasses = File(sourceRoot, "passes")
        val sourcePass = File(sourcePasses, "pass-1")
        sourcePass.mkdirs()
        File(sourcePass, "main.json").writeText("pass-data")
        val sourceState = File(sourceRoot, "state")
        sourceState.mkdirs()
        File(sourceState, "archive_state.json").writeText("state-data")
        val archive = ByteArrayOutputStream()
        BackupArchive.exportBackup(
            passesDir = sourcePasses,
            stateDir = sourceState,
            preferences = mapOf("nightmode" to "auto"),
            appVersionName = "26.4.29",
            appVersionCode = 202604290,
            outputStream = archive,
        )

        val targetRoot = temporaryFolder.newFolder()
        val restoredPreferences = mutableMapOf<String, Any>()
        BackupArchive.importBackup(
            inputStream = ByteArrayInputStream(archive.toByteArray()),
            passesDir = File(targetRoot, "passes"),
            stateDir = File(targetRoot, "state"),
            restorePreferences = { restoredPreferences.putAll(it) },
        )

        assertThat(File(targetRoot, "passes/pass-1/main.json").readText()).isEqualTo("pass-data")
        assertThat(File(targetRoot, "state/archive_state.json").readText()).isEqualTo("state-data")
        assertThat(restoredPreferences).containsEntry("nightmode", "auto")
    }

    @Test
    fun importRejectsUnsafeEntryPath() {
        val archive = zipOf("passes/../escape.txt" to "bad")
        val targetRoot = temporaryFolder.newFolder()

        assertImportRejected(archive, targetRoot)
    }

    @Test
    fun importRejectsOversizedPreferencesEntry() {
        val archive = zipOf("preferences.json" to "{\"big\":\"${"x".repeat(300 * 1024)}\"}")
        val targetRoot = temporaryFolder.newFolder()

        assertImportRejected(archive, targetRoot)
    }

    @Test
    fun recognizesPassWalletBackupManifest() {
        val archive = ByteArrayOutputStream()
        BackupArchive.exportBackup(
            passesDir = temporaryFolder.newFolder("summary-passes"),
            stateDir = temporaryFolder.newFolder("summary-state"),
            preferences = emptyMap(),
            appVersionName = "26.5.5",
            appVersionCode = 202605050,
            outputStream = archive,
        )

        val summary = BackupArchive.readSummary(ByteArrayInputStream(archive.toByteArray()))

        assertThat(summary?.app).isEqualTo("PassWallet")
        assertThat(summary?.appVersionName).isEqualTo("26.5.5")
        assertThat(BackupArchive.looksLikePassWalletBackup(ByteArrayInputStream(archive.toByteArray()))).isTrue()
    }

    @Test
    fun doesNotTreatPkpassManifestAsBackup() {
        val archive = zipOf("manifest.json" to "{\"pass.json\":\"abc123\"}", "pass.json" to "{}")

        assertThat(BackupArchive.looksLikePassWalletBackup(ByteArrayInputStream(archive))).isFalse()
    }

    @Test
    fun listsThePassesContainedInABackup() {
        val archive = zipOf(
            "manifest.json" to "{}",
            "passes/pass-1/main.json" to "{}",
            "passes/pass-1/logo.png" to "png",
            "passes/pass-2/pass.json" to "{}",
            "state/archive_state.json" to "{}",
            "preferences.json" to "{}",
        )

        assertThat(BackupArchive.passIds(ByteArrayInputStream(archive))).containsOnly("pass-1", "pass-2")
    }

    private fun assertImportRejected(archive: ByteArray, targetRoot: File) {
        try {
            BackupArchive.importBackup(
                inputStream = ByteArrayInputStream(archive),
                passesDir = File(targetRoot, "passes"),
                stateDir = File(targetRoot, "state"),
                restorePreferences = {},
            )
            fail("Expected backup import to reject unsafe archive")
        } catch (expected: IllegalArgumentException) {
            if (expected.message.isNullOrBlank()) {
                fail("Expected rejection to include an error message")
            }
        }
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun zipEntries(bytes: ByteArray): Map<String, String> {
        val result = linkedMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    result[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
            }
        }
        return result
    }
}
