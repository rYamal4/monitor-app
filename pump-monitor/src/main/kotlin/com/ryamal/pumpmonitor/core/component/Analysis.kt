package com.ryamal.pumpmonitor.core.component

data class Analysis(
    val recording: Recording,
    val intervals: List<Interval>,
    // соседние интервалы с одинаковым состоянием, склеенные для графика
    val segments: List<Interval>,
    val totals: Totals,
)
