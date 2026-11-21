package org.ligi.passandroid.reader

import android.content.Context
import android.graphics.Color
import org.json.JSONException
import org.json.JSONObject
import org.ligi.kaxt.parseColor
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.PassGrouping
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.BoardingTransitType
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField
import org.ligi.passandroid.model.pass.PassLocation
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.model.pass.PassVisualClassifier
import org.ligi.passandroid.model.pass.labelRes
import org.threeten.bp.DateTimeException
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.charset.Charset

/**
 * Reads an extracted pkpass (pass.json, pass.strings of the language, pictures) into a [Pass].
 */
object PkpassReader {

    private val PICTURES = listOf(
        PassBitmapDefinitions.BITMAP_ICON,
        PassBitmapDefinitions.BITMAP_LOGO,
        PassBitmapDefinitions.BITMAP_STRIP,
        PassBitmapDefinitions.BITMAP_THUMBNAIL,
        PassBitmapDefinitions.BITMAP_FOOTER,
    )

    /** the pkpass field groups in the order their fields are listed - back fields are hidden */
    private val FIELD_GROUPS = listOf("primaryFields", "headerFields", "secondaryFields", "auxiliaryFields")
    private const val BACK_FIELDS = "backFields"

    /**
     * Reads the pass in [passDirectory] (its name is the pass id), texts in [language] where the pass is translated.
     * Puts the best version of each picture where the pass expects it. Returns null without a readable pass.json.
     */
    fun read(passDirectory: File, language: String, context: Context): Pass? {
        val localizedPath = findLocalizedPath(passDirectory, language)
        val translation = PkpassTranslation()
        localizedPath?.let { translation.loadFromFile(File(it, "pass.strings")) }

        PICTURES.forEach { takeBestPicture(passDirectory, localizedPath, it) }

        val passJSON = readPassJson(File(passDirectory, "pass.json"))
        if (passJSON == null) {
            Timber.w("could not load pass.json from pass")
            return null
        }

        val pass = Pass(passDirectory.name)

        pass.barCode = readBarcode(passJSON)
        readDate(passJSON, "relevantDate")?.let { pass.calendarTimespan = Pass.TimeSpan(from = it) }
        readDate(passJSON, "expirationDate")?.let { pass.validTimespans = listOf(Pass.TimeSpan(to = it)) }

        pass.serial = passJSON.stringOrNull("serialNumber")
        pass.authToken = passJSON.stringOrNull("authenticationToken")
        pass.webServiceURL = passJSON.stringOrNull("webServiceURL")
        pass.passIdent = passJSON.stringOrNull("passTypeIdentifier")
        pass.grouping = PassGrouping.fromPassbook(pass.passIdent, passJSON.stringOrNull("groupingIdentifier"))

        pass.locations = readLocations(passJSON, translation)

        passJSON.stringOrNull("backgroundColor")?.let { pass.accentColor = it.parseColor(Color.BLACK) }
        pass.logoText = passJSON.stringOrNull("logoText")?.let { translation.translate(it) }?.takeIf { it.isNotBlank() }
        pass.foregroundColorValue = passJSON.stringOrNull("foregroundColor")
        pass.labelColorValue = passJSON.stringOrNull("labelColor")
        passJSON.stringOrNull("description")?.let { pass.description = translation.translate(it) }

        readTypeAndFields(pass, passJSON, translation, context)

        passJSON.stringOrNull("organizationName")?.let { pass.creator = it }

        PkpassQuirkCorrector().correctQuirks(pass)
        pass.visualType = PassVisualClassifier.classify(pass)

        return pass
    }

    /** pass.json in the detected encoding - or, if that fails, in the first charset it can be read with */
    private fun readPassJson(file: File): JSONObject? {
        try {
            readJSONSafely(PkpassTranslation.readFileAsStringGuessEncoding(file))?.let { return it }
        } catch (e: Exception) {
            Timber.i("PassParse Exception: $e")
        }

        // I had got a strange passbook with UCS-2 which could not be parsed before
        // was searching for a auto-detection, but could not find one with support for this encoding
        // and the right license
        for (charset in Charset.availableCharsets().values) {
            try {
                readJSONSafely(file.bufferedReader(charset).readText())?.let { return it }
            } catch (ignored: Exception) {
                // we try with next charset
            }
        }
        return null
    }

    /** the (first) barcode - null when there is none or it cannot be read */
    private fun readBarcode(passJSON: JSONObject): BarCode? = try {
        passJSON.getBarcodeJson()?.let { barcodeJSON ->
            BarCode(BarCode.getFormatFromString(barcodeJSON.getString("format")), barcodeJSON.getString("message")).apply {
                barcodeJSON.stringOrNull("altText")?.let { alternativeText = it }
            }
        }
    } catch (ignored: Exception) {
        // a broken barcode should not cost the whole pass
        null
    }

    /** be robust when it comes to bad dates - had a RL crash with "2013-12-25T00:00-57:00" here: the pass has no date then */
    private fun readDate(passJSON: JSONObject, key: String): ZonedDateTime? = try {
        passJSON.stringOrNull(key)?.let { ZonedDateTime.parse(it) }
    } catch (e: DateTimeException) {
        null
    }

    /** the locations up to the first broken one */
    private fun readLocations(passJSON: JSONObject, translation: PkpassTranslation): List<PassLocation> {
        val locations = ArrayList<PassLocation>()
        try {
            val locationsJSON = passJSON.getJSONArray("locations")
            for (i in 0 until locationsJSON.length()) {
                val obj = locationsJSON.getJSONObject(i)
                locations.add(PassLocation().apply {
                    lat = obj.getDouble("latitude")
                    lon = obj.getDouble("longitude")
                    obj.stringOrNull("relevantText")?.let { name = translation.translate(it) }
                })
            }
        } catch (ignored: JSONException) {
        }
        return locations
    }

    /**
     * The type is the pkpass style found in pass.json (with several, the last of [PassType.pkpassStyles]);
     * the fields of that style are read, plus a field naming the type.
     */
    private fun readTypeAndFields(pass: Pass, passJSON: JSONObject, translation: PkpassTranslation, context: Context) {
        PassType.pkpassStyles.lastOrNull { passJSON.has(it.pkpassStyleKey) }?.let { pass.type = it }

        val styleJSON = pass.type.pkpassStyleKey?.let { passJSON.optJSONObject(it) } ?: return
        if (pass.type == PassType.PKBOARDING || pass.type == PassType.BOARDING) {
            pass.boardingTransitType = readBoardingTransitType(passJSON)
        }

        val fields = ArrayList<PassField>()
        FIELD_GROUPS.forEach { fields += readFields(styleJSON, it, translation, hide = false) }
        fields += readFields(styleJSON, BACK_FIELDS, translation, hide = true)
        fields.add(PassField("", context.getString(R.string.type), context.getString(pass.type.labelRes), false))
        pass.fields = fields
    }

    private fun readFields(styleJSON: JSONObject, fieldsName: String, translation: PkpassTranslation, hide: Boolean): List<PassField> {
        val jsonArray = styleJSON.optJSONArray(fieldsName) ?: return emptyList()
        return (0 until jsonArray.length()).mapNotNull { i ->
            val jsonObject = jsonArray.optJSONObject(i)
            if (jsonObject == null) {
                Timber.w("could not process PassField from JSON for $fieldsName")
                return@mapNotNull null
            }
            PassField(
                key = jsonObject.stringOrNull("key")?.let { translation.translate(it) },
                label = jsonObject.stringOrNull("label")?.let { translation.translate(it) },
                value = jsonObject.stringOrNull("value")?.let { translation.translate(it) },
                hide = hide,
                hint = fieldsName,
            )
        }
    }

    private fun readBoardingTransitType(passJSON: JSONObject): BoardingTransitType {
        val boardingJson = passJSON.optJSONObject("boardingPass") ?: return BoardingTransitType.GENERIC
        return BoardingTransitType.fromPassKitName(boardingJson.stringOrNull("transitType"))
    }

    /** the lproj directory of [language], or of English as fallback */
    private fun findLocalizedPath(passDirectory: File, language: String): File? =
        listOf("$language.lproj", "en.lproj")
            .map { File(passDirectory, it) }
            .firstOrNull { it.isDirectory }

    /**
     * Copies the best version of a picture to `<name>.png` in [passDirectory], where the pass reads it:
     * localized high resolution, localized, high resolution. Without any of them a `<name>.png` that is already
     * there stays.
     */
    private fun takeBestPicture(passDirectory: File, localizedPath: File?, name: String) {
        val target = File(passDirectory, name + Pass.FILETYPE_IMAGES)
        val candidates = listOfNotNull(
            localizedPath?.let { File(it, "$name@2x.png") },
            localizedPath?.let { File(it, "$name.png") },
            File(passDirectory, "$name@2x.png"),
        )
        val best = candidates.firstOrNull { it.isFile } ?: return
        try {
            best.copyTo(target, overwrite = true)
        } catch (e: IOException) {
            Timber.w(e, "could not persist pass bitmap")
        }
    }
}

/** the value of [key] as string - null when it is missing or JSON null */
internal fun JSONObject.stringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key) else null
