package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.ligi.passandroid.R
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.importer.FallbackImport
import org.ligi.passandroid.importer.ImportError
import org.ligi.passandroid.importer.PassImporter
import org.mockito.Mockito.mock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Importing pass files into the passes directory, tested with real archives on a temporary directory.
 */
class ThePassImporter {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val passesDir by lazy { File(temporaryFolder.root, "passes") }
    private val workDir by lazy { temporaryFolder.newFolder("work") }

    private fun importer(fallback: FallbackImport = FallbackImport { null }) =
        PassImporter(passesDir, workDir, fallback, mock(Tracker::class.java))

    private fun import(bytes: ByteArray, overwrite: Boolean = false, fallback: FallbackImport = FallbackImport { null }) =
        importer(fallback).import(ByteArrayInputStream(bytes), source = "test-source", overwrite = overwrite)

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun pkpass(id: String, passJson: String = "{}") = zipOf(
        "manifest.json" to """{"pass.json":"$id"}""".toByteArray(),
        "pass.json" to passJson.toByteArray(),
    )

    // --- pass formats

    @Test
    fun importsAPkpassUnderItsManifestId() {
        val result = import(pkpass("abc123", """{"description":"Concert"}"""))

        assertThat(result.passIds).containsExactly("abc123")
        assertThat(result.errors).isEmpty()
        assertThat(File(passesDir, "abc123/pass.json").readText()).isEqualTo("""{"description":"Concert"}""")
        assertThat(File(passesDir, "abc123/source.obj").readText()).isEqualTo("test-source")
    }

    @Test
    fun importsAnEspassUnderItsId() {
        val result = import(zipOf("main.json" to """{"id":"es-1"}""".toByteArray()))

        assertThat(result.passIds).containsExactly("es-1")
        assertThat(File(passesDir, "es-1/main.json")).exists()
    }

    @Test
    fun importsAllPassesOfAPkpassesBundle() {
        val result = import(zipOf("one.pkpass" to pkpass("one"), "two.pkpass" to pkpass("two")))

        assertThat(result.passIds).containsExactly("one", "two")
        assertThat(result.errors).isEmpty()
    }

    @Test
    fun bundleImportsTheGoodPassesAndReportsEachBrokenOne() {
        val result = import(
            zipOf(
                "good.pkpass" to pkpass("good"),
                "broken.pkpass" to zipOf("something.txt" to "x".toByteArray()),
            )
        )

        assertThat(result.passIds).containsExactly("good")
        assertThat(result.errors).containsExactly(ImportError(R.string.import_error_no_manifest))
    }

    @Test
    fun bundleReportsPassesImportedBeforeALaterPassIsRejected() {
        val result = import(
            zipOf(
                "good.pkpass" to pkpass("good"),
                "huge.pkpass" to ByteArray(26 * 1024 * 1024),
            )
        )

        assertThat(result.passIds).containsExactly("good")
        assertThat(result.errors).containsExactly(ImportError(R.string.import_error_bundle_oversized_pass))
        assertThat(File(passesDir, "good")).exists()
    }

    // --- safety checks

    @Test
    fun rejectsArchiveWithPathOutsideTheTarget() {
        val result = import(zipOf("manifest.json" to """{"pass.json":"x"}""".toByteArray(), "../evil.txt" to "x".toByteArray()))

        assertThat(result.passIds).isEmpty()
        assertThat(result.errors).containsExactly(ImportError(R.string.import_error_unsafe_path))
        assertThat(passesDir.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun rejectsInvalidPassId() {
        val result = import(pkpass("../escape"))

        assertThat(result.errors).containsExactly(ImportError(R.string.import_error_invalid_pass_id))
        assertThat(passesDir.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun rejectsArchiveWithTooManyFiles() {
        val entries = (0..600).map { "file$it.txt" to "x".toByteArray() }.toTypedArray()

        val result = import(zipOf(*entries))

        assertThat(result.errors).containsExactly(ImportError(R.string.import_error_too_many_files))
    }

    // --- update vs duplicate

    @Test
    fun normalImportKeepsAnExistingPassWithTheSameId() {
        import(pkpass("same", "old"))

        val result = import(pkpass("same", "new"))

        assertThat(result.passIds).containsExactly("same")
        assertThat(File(passesDir, "same/pass.json").readText()).isEqualTo("old")
    }

    @Test
    fun updateReplacesAnExistingPass() {
        import(pkpass("same", "old"))

        val result = import(pkpass("same", "new"), overwrite = true)

        assertThat(result.passIds).containsExactly("same")
        assertThat(File(passesDir, "same/pass.json").readText()).isEqualTo("new")
    }

    // --- unknown format

    @Test
    fun fileThatIsNeitherPassNorImageIsUnknown() {
        val result = import("just text".toByteArray())

        assertThat(result.passIds).isEmpty()
        assertThat(result.errors).containsExactly(ImportError(R.string.import_error_unknown_format))
    }

    @Test
    fun fileThatIsNoArchiveIsHandedToTheImageImport() {
        val result = import("image bytes".toByteArray(), fallback = FallbackImport { file ->
            if (file.readText() == "image bytes") "image-pass" else null
        })

        assertThat(result.passIds).containsExactly("image-pass")
        assertThat(result.errors).isEmpty()
    }
}
