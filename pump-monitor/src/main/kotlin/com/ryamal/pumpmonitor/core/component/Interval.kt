package com.ryamal.pumpmonitor.core.component

import java.math.BigDecimal

sealed interface IntervalState {
    data object Gap : IntervalState

    data object Invalid : IntervalState

    data object Stopped : IntervalState

    data class Working(val mode: Mode) : IntervalState
}

// интервал [start, end), состояние берётся по левой записи
data class Interval(
    val start: BigDecimal,
    val end: BigDecimal,
    val state: IntervalState,
) {
    val duration: BigDecimal
        get() = end - start
}
