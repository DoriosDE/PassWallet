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
     * null = not decided yet (may be backfilled from the pkpass groupingIdentifier),
     * empty = explicitly not grouped.
     */
    var groupId: String?

    /** text shown next to / instead of the logo (pkpass "logoText") */
    var logoText: String?

    /** pkpass "foregroundColor" / "labelColor" as found in the pass (e.g. "rgb(1,2,3)" or "#010203") */
    var foregroundColorValue: String?
    var labelColorValue: String?

    /** version of the list appearance data above - older passes get it backfilled from pass.json */
    var appearanceVersion: Int

    val app: String?

    val validTimespans: List<PassImpl.TimeSpan>?
    var calendarTimespan: PassImpl.TimeSpan?

    fun getBitmap(passStore: PassStore, passBitmap: String): Bitmap?

}
