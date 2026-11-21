package org.ligi.passandroid.ui.edit

import android.app.Activity
import android.net.Uri
import org.ligi.kaxt.loadImage
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassImpl
import timber.log.Timber
import java.io.File
import java.io.IOException

class ImageEditHelper(private val context: Activity, private val passStore: PassStore) {

    fun importImage(pass: Pass, uri: Uri, @Pass.PassBitmap name: String) {
        val extractedFile = uri.loadImage(context)
        if (extractedFile != null && extractedFile.exists()) {
            try {
                val destinationFile = File(passStore.directoryOf(pass.id), name + PassImpl.FILETYPE_IMAGES)
                destinationFile.delete()
                extractedFile.copyTo(destinationFile)
            } catch (e: IOException) {
                Timber.w(e, "could not import pass image")
            }
        }
    }
}
