package org.ligi.passandroid.ui.rendering

import org.ligi.passandroid.R
import org.ligi.passandroid.model.pass.BoardingTransitType

object BoardingTransitIcon {

    fun drawableFor(transitType: BoardingTransitType): Int {
        return when (transitType) {
            BoardingTransitType.AIR -> R.drawable.ic_transit_air_24
            BoardingTransitType.BOAT -> R.drawable.ic_transit_boat_24
            BoardingTransitType.BUS -> R.drawable.ic_transit_bus_24
            BoardingTransitType.TRAIN -> R.drawable.ic_transit_train_24
            BoardingTransitType.GENERIC -> R.drawable.ic_transit_generic_24
        }
    }
}
