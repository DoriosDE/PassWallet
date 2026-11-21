package org.ligi.passandroid.importer

import android.content.Context
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * An opened file or download to import passes from.
 *
 * @param source where the passes came from - stored with the imported passes
 */
class ImportInput(val source: String, val inputStream: InputStream) {
    companion object {
        /** opens [uri] (content, file, http or https) - null when there is nothing to read */
        fun open(context: Context, uri: Uri): ImportInput? {
            return when (uri.scheme) {
                "content" -> openContent(context, uri)
                "http", "https" -> openDownload(uri)
                else -> openUrlConnection(uri)
            }
        }
    }
}

private const val IPHONE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 7_0 like Mac OS X; en-us) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53"

/** providers known to hide pass downloads from Android user agents - we pretend to be an iPhone for them */
private val IPHONE_ONLY_PROVIDERS = listOf(
    "//m.aircanada.ca/ebp/",
    "//services.aircanada.com/ebp/",
    "//mci.aircanada.com/mci/bp/",
    "//checkin.si.amadeus.net",
    "//mbk.thy.com/",
    "//passbook.heathrow.com/",
    "//www.eventbrite.com/passes/order",
)

private val importHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .build()

private fun openDownload(uri: Uri): ImportInput? {
    val url = URL("$uri")
    val requestBuilder = Request.Builder().url(url)

    if (IPHONE_ONLY_PROVIDERS.any { "$uri".contains(it) }) {
        requestBuilder.header("User-Agent", IPHONE_USER_AGENT)
    }

    val response = importHttpClient.newCall(requestBuilder.build()).execute()
    if (!response.isSuccessful) {
        response.close()
        return null
    }

    val body = response.body()
    if (body == null) {
        response.close()
        return null
    }

    return ImportInput("$uri", ResponseClosingInputStream(body.byteStream(), response))
}

private fun openContent(ctx: Context, uri: Uri): ImportInput? {
    return ctx.contentResolver.openInputStream(uri)?.let { ImportInput("$uri", it) }
}

private fun openUrlConnection(uri: Uri): ImportInput? {
    val connection = URL("$uri").openConnection().apply {
        connectTimeout = 10_000
        readTimeout = 30_000
    }
    return ImportInput("$uri", BufferedInputStream(connection.getInputStream(), 4096))
}

private class ResponseClosingInputStream(
    inputStream: InputStream,
    private val response: Response,
) : FilterInputStream(inputStream) {
    override fun close() {
        try {
            super.close()
        } finally {
            response.close()
        }
    }
}
