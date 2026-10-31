package com.ryamal.pumpmonitor.core.component

import java.math.BigDecimal

data class Sample(
    // BigDecimal, чтобы разность меток считалась точно: в double 2.2 - 0.7 != 1.5
    val t: BigDecimal,
    val pressureMpa: Double?,
    val rpm: Double?,
    val valid: Boolean,
)
