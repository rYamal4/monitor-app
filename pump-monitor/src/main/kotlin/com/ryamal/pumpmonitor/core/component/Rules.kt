package com.ryamal.pumpmonitor.core.component

import java.math.BigDecimal

enum class Mode(val id: String) {
    LOW("low"),
    NORMAL("normal"),
    HIGH("high"),
    ;

    companion object {
        fun byId(id: String): Mode = entries.firstOrNull { it.id == id } ?: error("Неизвестный режим: $id")
    }
}

// давление от pressureFrom включительно до pressureTo не включительно, pressureTo = null - без верхней границы
data class ModeRange(
    val mode: Mode,
    val pressureFrom: Double,
    val pressureTo: Double?,
)

data class Rules(
    val maxIntervalS: BigDecimal,
    val rpmMin: Double,
    val pressureMin: Double,
    val modes: List<ModeRange>,
)
