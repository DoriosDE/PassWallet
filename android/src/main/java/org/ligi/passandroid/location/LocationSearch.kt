package org.ligi.passandroid.location

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LocationSearchResult(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
)

/** Looks up places by name with the OpenStreetMap Nominatim search API. */
class LocationSearch(
    private val client: OkHttpClient = OkHttpClient(),
    private val searchUrl: String = NOMINATIM_SEARCH_URL,
) {

    /**
     * Returns the places found for [query], with names in [language] (an IETF language tag) where available.
     * Throws an [IOException] when the request fails or the answer is not a list of places.
     * Cancelling the coroutine cancels the request.
     */
    suspend fun search(query: String, language: String): List<LocationSearchResult> {
        val request = Request.Builder()
            .url(searchUrlFor(query))
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", language)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val results = runCatching {
                        response.use {
                            if (!it.isSuccessful) throw IOException("Location search failed with HTTP ${it.code()}")
                            parseNominatimResults(it.body()?.string().orEmpty())
                        }
                    }
                    results.fold(continuation::resume, continuation::resumeWithException)
                }
            })
        }
    }

    internal fun searchUrlFor(query: String): HttpUrl = HttpUrl.get(searchUrl).newBuilder()
        .addQueryParameter("format", "jsonv2")
        .addQueryParameter("limit", RESULT_LIMIT.toString())
        .addQueryParameter("q", query)
        .build()

    companion object {
        const val NOMINATIM_SEARCH_URL = "https://nominatim.openstreetmap.org/search"
        private const val USER_AGENT = "PassWallet Android (org.baumweg.passwallet)"
        private const val RESULT_LIMIT = 5
    }
}

@JsonClass(generateAdapter = true)
internal data class NominatimPlace(
    val lat: String?,
    val lon: String?,
    @Json(name = "display_name") val displayName: String?,
)

private val nominatimPlacesAdapter = Moshi.Builder().build()
    .adapter<List<NominatimPlace>>(Types.newParameterizedType(List::class.java, NominatimPlace::class.java))

/**
 * Reads the places of a Nominatim jsonv2 search answer, skipping places without a name or valid coordinates.
 * Throws an [IOException] when [json] is not a list of places.
 */
internal fun parseNominatimResults(json: String): List<LocationSearchResult> {
    val places = try {
        nominatimPlacesAdapter.fromJson(json)
    } catch (e: JsonDataException) {
        throw IOException("Unexpected location search answer", e)
    } ?: throw IOException("Empty location search answer")

    return places.mapNotNull { place ->
        val latitude = place.lat?.toDoubleOrNull() ?: return@mapNotNull null
        val longitude = place.lon?.toDoubleOrNull() ?: return@mapNotNull null
        val displayName = place.displayName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        LocationSearchResult(displayName, latitude, longitude)
    }
}
