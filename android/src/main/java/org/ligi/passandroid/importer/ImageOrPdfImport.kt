package org.ligi.passandroid.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import okio.buffer
import okio.source
import org.ligi.passandroid.model.createPassForImageImport
import org.ligi.passandroid.model.createPassForPDFImport
import org.ligi.passandroid.model.ArchiveState
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * Turns an image or the first page of a PDF into a simple pass that shows it (Android adapter of [FallbackImport]).
 */
class ImageOrPdfImport(private val context: Context, private val passStore: PassStore) : FallbackImport {

    override fun import(file: File): String? = importImage(file) ?: importPdf(file)

    private fun importImage(file: File): String? {
        BitmapFactory.decodeFile(file.absolutePath) ?: return null

        return storePassShowing(createPassForImageImport(context.resources)) { strip -> file.copyTo(strip) }
    }

    private fun importPdf(file: File): String? = try {
        if (file.source().buffer().use { it.readUtf8(4) } != "%PDF") {
            null
        } else {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    renderer.openPage(0).use { page ->
                        val widthPixels = context.resources.displayMetrics.widthPixels
                        val ratio = page.height.toFloat() / page.width
                        val pdfBitmap = createBitmap(widthPixels, (widthPixels * ratio).toInt())
                        page.render(pdfBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        storePassShowing(createPassForPDFImport(context.resources)) { strip ->
                            FileOutputStream(strip).use { pdfBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        Timber.w(e, "could not inspect PDF import")
        null
    }

    /** stores [pass] as active pass; [writeStrip] writes the picture it shows (strip.png) */
    private fun storePassShowing(pass: Pass, writeStrip: (File) -> Unit): String {
        val passDir = passStore.getPathForID(pass.id)
        passDir.mkdirs()
        writeStrip(File(passDir, "strip.png"))

        passStore.save(pass)
        passStore.archive.setState(pass, ArchiveState.ACTIVE)
        return pass.id
    }
}
