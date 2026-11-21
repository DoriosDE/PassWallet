package org.ligi.passandroid.model.pass

import android.graphics.Bitmap
import androidx.annotation.StringDef
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_FOOTER
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_ICON
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_LOGO
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_STRIP
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_THUMBNAIL
import org.ligi.passandroid.model.PassGrouping
import org.ligi.passandroid.model.PassStore


interface Pass {

    @StringDef(BITMAP_ICON, BITMAP_THUMBNAIL, BITMAP_STRIP, BITMAP_LOGO, BITMAP_FOOTER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class PassBitmap

    val description: String?

    var type: PassType

    var boardingTransitType: BoardingTransitType

    var visualType: PassVisualType

    val fields: List<PassField>

    val locations: List<PassLocation>

    var accentColor: Int

    val id: String

    val creator: String?

    fun getSource(passStore: PassStore): String?

    var barCode: BarCode?

    val webServiceURL: String?

    val authToken: String?

    val serial: String?

    val passIdent: String?

    /** the stack of passes this pass belongs to in the pass list */
    var grouping: PassGrouping

    /** text shown next to / instead of the logo (pkpass "logoText") */
    var logoText: String?

    /** pkpass "foregroundColor" / "labelColor" as found in the pass (e.g. "rgb(1,2,3)" or "#010203") */
    var foregroundColorValue: String?
    var labelColorValue: String?

    val app: String?

    val validTimespans: List<PassImpl.TimeSpan>?
    var calendarTimespan: PassImpl.TimeSpan?

    fun getBitmap(passStore: PassStore, passBitmap: String): Bitmap?

}
