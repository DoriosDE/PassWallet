package org.ligi.passandroid.model.pass

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.StringDef
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonQualifier
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_FOOTER
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_ICON
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_LOGO
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_STRIP
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_THUMBNAIL
import org.ligi.passandroid.model.PassGrouping
import org.ligi.passandroid.model.PassStore
import org.threeten.bp.ZonedDateTime
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*

@JsonClass(generateAdapter = true)
/**
 * A pass: its fields, barcode, colors and the data needed to update it. Stored as main.json in the pass directory,
 * next to its pictures.
 */
class Pass(val id: String) {

    @StringDef(BITMAP_ICON, BITMAP_THUMBNAIL, BITMAP_STRIP, BITMAP_LOGO, BITMAP_FOOTER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class PassBitmap

    @Retention(AnnotationRetention.RUNTIME)
    @JsonQualifier
    annotation class HexColor

    var creator: String? = null

    var type: PassType = PassType.EVENT

    var boardingTransitType: BoardingTransitType = BoardingTransitType.GENERIC

    var visualType: PassVisualType = PassVisualType.DEFAULT

    var barCode: BarCode? = null

    @field:[HexColor]
    var accentColor: Int = 0

    var description: String? = null
        get() {
            if (field == null) {
                return "" // better way of returning no description - so we can avoid optional / null checks and it is kind of the same thing
                // an navigation_drawer_header description - we can do kind of all String operations safely this way and do not have to care about the existence of a real description
                // if we want to know if one is there we can check length for being 0 still ( which we would have to do anyway for navigation_drawer_header descriptions )
                // See no way at the moment where we would have to distinguish between an navigation_drawer_header and an missing description
            }
            return field
        }

    @JsonClass(generateAdapter = true)
    class TimeRepeat(val offset: Int, val count: Int)

    @JsonClass(generateAdapter = true)
    class TimeSpan(val from: ZonedDateTime? = null, val to: ZonedDateTime? = null, val repeat: TimeRepeat? = null)

    var validTimespans: List<TimeSpan> = ArrayList()

    var calendarTimespan: TimeSpan? = null

    var fields: MutableList<PassField> = ArrayList()

    var locations: List<PassLocation> = ArrayList()

    var app: String? = null

    var authToken: String? = null

    var webServiceURL: String? = null

    var serial: String? = null

    var passIdent: String? = null

    /** the stack of passes this pass belongs to in the pass list */
    var grouping: PassGrouping = PassGrouping.None

    /** text shown next to / instead of the logo (pkpass "logoText") */
    var logoText: String? = null

    /** pkpass "foregroundColor" / "labelColor" as found in the pass (e.g. "rgb(1,2,3)" or "#010203") */
    var foregroundColorValue: String? = null

    var labelColorValue: String? = null

    fun getBitmap(passStore: PassStore, @PassBitmap passBitmap: String): Bitmap? {
        return try {
            val file = File(passStore.directoryOf(id), passBitmap + FILETYPE_IMAGES)
            BitmapFactory.decodeStream(FileInputStream(file))
        } catch (expectedInSomeCases_willJustReturnNull: FileNotFoundException) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }

    }

    fun getSource(passStore: PassStore): String? {
        val file = File(passStore.directoryOf(id), "source.txt")

        if (file.exists()) {
            return file.bufferedReader().readText()
        }
        return null
    }

    override fun toString(): String {
        return "ID=$id"
    }

    companion object {
        const val FILETYPE_IMAGES = ".png"
    }

}
