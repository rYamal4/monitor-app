package com.ryamal.pumpmonitor.core.component

import java.math.BigDecimal

class Recording(
    val datasetId: String,
    val samples: List<Sample>,
) {
    init {
        samples.zipWithNext().forEachIndexed { i, (previous, current) ->
            require(current.t > previous.t) { "samples[${i + 1}].t_s не больше предыдущей метки" }
        }
    }

    val span: BigDecimal
        get() = if (samples.size < 2) BigDecimal.ZERO else samples.last().t - samples.first().t
}
