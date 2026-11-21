package org.ligi.passandroid.model.pass

import android.graphics.Bitmap
import androidx.annotation.StringDef
import org.ligi.passandroid.model.PassBitmapDefinitions.*
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

    /**
     * Identifies passes that belong together and are shown as one stack in the pass list.
     * null = no group, [org.ligi.passandroid.model.PassGroups.USER_UNGROUPED] = taken out of a group by the user.
     */
    var groupId: String?

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
