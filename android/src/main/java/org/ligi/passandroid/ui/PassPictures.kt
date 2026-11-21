package org.ligi.passandroid.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import java.io.File

/** the picture [passBitmap] of the pass (e.g. its logo) - null when the pass has none or it cannot be loaded */
fun Pass.getBitmap(passStore: PassStore, @Pass.PassBitmap passBitmap: String): Bitmap? {
    val file = File(passStore.directoryOf(id), passBitmap + Pass.FILETYPE_IMAGES)
    if (!file.exists()) return null
    return try {
        BitmapFactory.decodeFile(file.path)
    } catch (e: OutOfMemoryError) {
        null
    }
}
