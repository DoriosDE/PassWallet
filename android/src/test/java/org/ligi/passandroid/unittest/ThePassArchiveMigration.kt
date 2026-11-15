package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.PassArchive
import org.ligi.passandroid.model.pass.PassImpl

class ThePassArchiveMigration {

    private val passes = listOf("a", "b", "c", "t", "x", "g", "n", "r").associateWith { PassImpl(it) }

    @Test
    fun trashIsArchivedAndCategoriesBecomeGroups() {
        passes.getValue("g").groupId = "pk:existing"
        val topics = mutableMapOf(
            "a" to "Concert", "b" to "Concert", "g" to "Concert",
            "c" to "Single",
            "t" to "trash",
            "n" to PassArchive.ACTIVE,
            "r" to PassArchive.ARCHIVE,
        )

        val changed = PassArchive.migrateLegacyTopics(topics, passes::get)

        assertThat(topics).containsEntry("a", PassArchive.ACTIVE)
            .containsEntry("b", PassArchive.ACTIVE)
            .containsEntry("g", PassArchive.ACTIVE)
            .containsEntry("c", PassArchive.ACTIVE)
            .containsEntry("t", PassArchive.ARCHIVE)
            .containsEntry("n", PassArchive.ACTIVE)
            .containsEntry("r", PassArchive.ARCHIVE)

        assertThat(changed).containsOnly(passes["a"], passes["b"]).hasSize(2)
        assertThat(passes.getValue("a").groupId).isNotEmpty().isEqualTo(passes.getValue("b").groupId)
        assertThat(passes.getValue("g").groupId).isEqualTo("pk:existing")
        assertThat(passes.getValue("c").groupId).isNull()
    }

    @Test
    fun migrationIsIdempotent() {
        val topics = mutableMapOf("a" to PassArchive.ACTIVE, "r" to PassArchive.ARCHIVE)

        assertThat(PassArchive.migrateLegacyTopics(topics, passes::get)).isEmpty()
        assertThat(topics).containsEntry("a", PassArchive.ACTIVE).containsEntry("r", PassArchive.ARCHIVE)
    }
}
