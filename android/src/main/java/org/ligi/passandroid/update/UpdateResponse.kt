package org.ligi.passandroid.update

import okhttp3.Response
import okhttp3.ResponseBody

/** what the web service of a pass answered when asked for its latest version */
internal sealed class UpdateResponse {
    data class Available(val body: ResponseBody) : UpdateResponse()

    /** no new pass in the answer, e.g. 204 (unchanged), 404 or 410 (gone) */
    data class Unavailable(val statusCode: Int) : UpdateResponse()

    companion object {
        fun of(response: Response): UpdateResponse {
            val body = response.body()
            return if (response.isSuccessful && body != null && response.code() != 204) {
                Available(body)
            } else {
                Unavailable(response.code())
            }
        }
    }
}
