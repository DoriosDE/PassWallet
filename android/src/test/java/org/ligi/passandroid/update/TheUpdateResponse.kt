package org.ligi.passandroid.update

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test

class TheUpdateResponse {

    @Test
    fun httpNotFoundIsAHandledUnavailableUpdate() {
        val result = UpdateResponse.of(responseWithCode(404))

        assertEquals(UpdateResponse.Unavailable(404), result)
    }

    @Test
    fun emptySuccessfulResponseIsAHandledUnavailableUpdate() {
        val result = UpdateResponse.of(responseWithCode(204))

        assertEquals(UpdateResponse.Unavailable(204), result)
    }

    private fun responseWithCode(code: Int): Response {
        return Response.Builder()
            .request(Request.Builder().url("https://example.com/pass").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .build()
    }
}
