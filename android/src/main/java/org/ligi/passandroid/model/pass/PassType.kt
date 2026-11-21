package org.ligi.passandroid.model.pass

/**
 * @param pkpassStyleKey the key of the pass style in a pkpass pass.json (e.g. "eventTicket"), null for types pkpass does not know
 */
enum class PassType(val pkpassStyleKey: String?) {
    GENERIC("generic"),
    EVENT("eventTicket"),
    COUPON("coupon"),
    BOARDING(null),
    PKBOARDING("boardingPass"),
    LOYALTY("storeCard"),
    VOUCHER(null);

    companion object {
        /**
         * the types that can be read from a pkpass pass.json - in the order they are looked for,
         * so with several style keys in one pass.json the last one wins
         */
        val pkpassStyles: List<PassType> = listOf(COUPON, EVENT, PKBOARDING, GENERIC, LOYALTY)
    }
}
