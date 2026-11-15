package org.ligi.passandroid.functions

import android.app.Activity
import com.google.android.material.snackbar.Snackbar
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassClassifier
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.ui.TopicNames

fun moveWithUndoSnackbar(passClassifier: PassClassifier, pass: Pass, topic: String, activity: Activity) =
    moveWithUndoSnackbar(passClassifier, listOf(pass), topic, activity)

fun moveWithUndoSnackbar(passClassifier: PassClassifier, passes: List<Pass>, topic: String, activity: Activity) {
    val oldTopics = passes.associateWith { passClassifier.getTopic(it, "") }
    val message = activity.resources.getQuantityString(
        R.plurals.passes_moved_to_topic,
        passes.size,
        passes.size,
        TopicNames.displayName(activity, topic)
    )

    Snackbar.make(activity.window.decorView.findViewById(R.id.fam), message, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) {
                oldTopics.forEach { (pass, oldTopic) -> passClassifier.topicByIdMap[pass.id] = oldTopic }
                passClassifier.processDataChange()
            }
            .show()

    passes.forEach { passClassifier.topicByIdMap[it.id] = topic }
    passClassifier.processDataChange()
}
