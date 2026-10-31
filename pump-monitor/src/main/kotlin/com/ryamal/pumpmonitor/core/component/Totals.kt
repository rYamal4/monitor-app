package com.ryamal.pumpmonitor.core.component

import java.math.BigDecimal

data class Totals(
    val spanS: BigDecimal,
    val workingS: BigDecimal,
    val stoppedS: BigDecimal,
    val gapS: BigDecimal,
    val invalidS: BigDecimal,
    val modesS: Map<Mode, BigDecimal>,
) {
    val unknownS: BigDecimal
        get() = gapS + invalidS
}
