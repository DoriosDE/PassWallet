package org.ligi.passandroid.model

import org.ligi.passandroid.model.pass.Pass

/**
 * Passes are either active (shown in the main list) or archived. The state is stored in the
 * [PassClassifier] (which used to hold free-form categories/topics).
 */
object PassArchive {
    /** stored value for active passes - "new" for compatibility with existing installations */
    const val ACTIVE = "new"
    const val ARCHIVE = "archive"

    private const val LEGACY_TRASH = "trash"
    private const val LEGACY_CATEGORY_GROUP_PREFIX = "manual:category:"

    fun isArchived(classifier: PassClassifier, pass: Pass) = classifier.getTopic(pass, ACTIVE) == ARCHIVE

    /**
     * Converts the old topic model to active/archive:
     * - passes from the trash are archived (nothing is deleted)
     * - passes from user created categories become active again and - as groups replace categories -
     *   the passes of each category that are not grouped yet are stacked into one group per category.
     *
     * Mutates [topicByIdMap] and returns the passes whose group changed and need to be saved.
     */
    fun migrateLegacyTopics(topicByIdMap: MutableMap<String, String>, passForId: (String) -> Pass?): List<Pass> {
        val changedPasses = ArrayList<Pass>()

        val legacyCategories = topicByIdMap.entries
            .filter { it.value != ACTIVE && it.value != ARCHIVE }
            .groupBy({ it.value }, { it.key })

        legacyCategories.forEach { (topic, ids) ->
            if (topic.equals(LEGACY_TRASH, ignoreCase = true)) {
                ids.forEach { topicByIdMap[it] = ARCHIVE }
                return@forEach
            }

            ids.forEach { topicByIdMap[it] = ACTIVE }

            val ungroupedPasses = ids.mapNotNull(passForId).filterNot { PassGroups.isGrouped(it) }
            if (ungroupedPasses.size >= 2) {
                ungroupedPasses.forEach {
                    it.groupId = LEGACY_CATEGORY_GROUP_PREFIX + topic
                    changedPasses.add(it)
                }
            }
        }

        return changedPasses
    }
}
