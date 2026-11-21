package org.ligi.passandroid.json

import com.squareup.moshi.Moshi

/** the Moshi setup for reading and writing passes (main.json) */
object PassWalletMoshi {
    fun create(): Moshi = Moshi.Builder()
        .add(ZonedTimeAdapter())
        .add(ColorAdapter())
        .add(PassGroupingAdapter())
        .build()
}
