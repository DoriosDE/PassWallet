package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.BoardingTransitType

class TheBoardingTransitType {

    @Test
    fun mapsAppleTransitTypes() {
        assertThat(BoardingTransitType.fromPassKitName("PKTransitTypeAir")).isEqualTo(BoardingTransitType.AIR)
        assertThat(BoardingTransitType.fromPassKitName("PKTransitTypeBoat")).isEqualTo(BoardingTransitType.BOAT)
        assertThat(BoardingTransitType.fromPassKitName("PKTransitTypeBus")).isEqualTo(BoardingTransitType.BUS)
        assertThat(BoardingTransitType.fromPassKitName("PKTransitTypeTrain")).isEqualTo(BoardingTransitType.TRAIN)
        assertThat(BoardingTransitType.fromPassKitName("PKTransitTypeGeneric")).isEqualTo(BoardingTransitType.GENERIC)
    }

    @Test
    fun defaultsUnknownTransitTypesToGeneric() {
        assertThat(BoardingTransitType.fromPassKitName(null)).isEqualTo(BoardingTransitType.GENERIC)
        assertThat(BoardingTransitType.fromPassKitName("wat")).isEqualTo(BoardingTransitType.GENERIC)
    }
}
