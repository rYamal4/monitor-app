package com.ryamal.pumpmonitor.core.operation.read

import com.ryamal.pumpmonitor.core.component.Analysis
import com.ryamal.pumpmonitor.core.component.Interval
import com.ryamal.pumpmonitor.core.component.IntervalState
import com.ryamal.pumpmonitor.core.component.Mode
import com.ryamal.pumpmonitor.core.component.Recording
import com.ryamal.pumpmonitor.core.component.Rules
import com.ryamal.pumpmonitor.core.component.Sample
import com.ryamal.pumpmonitor.core.component.Totals
import java.math.BigDecimal

class RecordingAnalyzer(private val rules: Rules) {

    fun analyze(recording: Recording): Analysis {
        val intervals = recording.samples.zipWithNext { left, right ->
            Interval(left.t, right.t, classify(left, right.t - left.t))
        }
        return Analysis(recording, intervals, segments(intervals), totals(intervals, recording.span))
    }

    // порядок проверок как в задании: пропуск, потом недостоверность, потом работа/остановка
    private fun classify(left: Sample, dt: BigDecimal): IntervalState {
        if (dt > rules.maxIntervalS) return IntervalState.Gap

        val pressure = left.pressureMpa
        val rpm = left.rpm
        if (!left.valid || pressure == null || rpm == null || pressure < 0 || rpm < 0) {
            return IntervalState.Invalid
        }

        return if (rpm >= rules.rpmMin && pressure >= rules.pressureMin) {
            IntervalState.Working(modeFor(pressure))
        } else {
            IntervalState.Stopped
        }
    }

    private fun modeFor(pressure: Double): Mode =
        rules.modes.firstOrNull { pressure >= it.pressureFrom && (it.pressureTo == null || pressure < it.pressureTo) }?.mode
            ?: error("Давление $pressure МПа не попадает ни в один режим из rules.json")

    private fun totals(intervals: List<Interval>, span: BigDecimal): Totals {
        fun sum(predicate: (IntervalState) -> Boolean): BigDecimal =
            intervals.filter { predicate(it.state) }.sumOf { it.duration }

        return Totals(
            spanS = span,
            workingS = sum { it is IntervalState.Working },
            stoppedS = sum { it == IntervalState.Stopped },
            gapS = sum { it == IntervalState.Gap },
            invalidS = sum { it == IntervalState.Invalid },
            modesS = Mode.entries.associateWith { mode -> sum { it == IntervalState.Working(mode) } },
        )
    }

    private fun segments(intervals: List<Interval>): List<Interval> =
        intervals.fold(mutableListOf()) { merged, interval ->
            val last = merged.lastOrNull()
            if (last != null && last.state == interval.state) {
                merged[merged.lastIndex] = last.copy(end = interval.end)
            } else {
                merged += interval
            }
            merged
        }
}
