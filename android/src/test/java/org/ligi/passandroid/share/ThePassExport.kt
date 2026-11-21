package org.ligi.passandroid.share

import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile

class ThePassExport {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun passDirectory() = temporaryFolder.newFolder("pass").apply {
        File(this, "main.json").writeText("""{"id":"pass"}""")
        File(this, "icon.png").writeBytes(byteArrayOf(1, 2, 3))
    }

    private fun entriesOf(file: File) = ZipFile(file).use { zip -> zip.entries().toList().associate { it.name to zip.getInputStream(it).readBytes() } }

    @Test
    fun packsTheFilesOfThePassWithoutRootFolder() {
        val target = File(temporaryFolder.root, "share/share.espass")

        exportPass(passDirectory(), target)

        val entries = entriesOf(target)
        assertThat(entries.keys).containsOnly("main.json", "icon.png")
        assertThat(String(entries.getValue("main.json"))).isEqualTo("""{"id":"pass"}""")
    }

    @Test
    fun replacesAnEarlierExport() {
        val target = File(temporaryFolder.root, "share.espass").apply { writeText("old export") }

        exportPass(passDirectory(), target)

        assertThat(entriesOf(target).keys).containsOnly("main.json", "icon.png")
    }

    @Test
    fun leavesNoFileWhenThePassIsMissing() {
        val target = File(temporaryFolder.root, "share.espass")

        val failure = runCatching { exportPass(File(temporaryFolder.root, "missing"), target) }.exceptionOrNull()

        assertThat(failure).isNotNull()
        assertThat(target).doesNotExist()
    }
}
