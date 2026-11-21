package org.ligi.passandroid.reader

import org.json.JSONObject


/** the barcode of a pass.json: "barcode", or the first of "barcodes" - null when there is none */
fun JSONObject.getBarcodeJson(): JSONObject? =
    optJSONObject("barcode") ?: optJSONArray("barcodes")?.optJSONObject(0)
