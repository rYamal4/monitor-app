package com.ryamal.pumpmonitor.core.operation.read

import com.ryamal.pumpmonitor.core.component.IntervalState
import com.ryamal.pumpmonitor.core.component.Mode
import com.ryamal.pumpmonitor.core.component.ModeRange
import com.ryamal.pumpmonitor.core.component.Recording
import com.ryamal.pumpmonitor.core.component.Rules
import com.ryamal.pumpmonitor.core.component.Sample
import com.ryamal.pumpmonitor.core.component.Totals
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordingAnalyzerTest {

    private val rules = Rules(
        maxIntervalS = BigDecimal("1.5"),
        rpmMin = 50.0,
        pressureMin = 1.0,
        modes = listOf(
            ModeRange(Mode.LOW, 1.0, 5.0),
            ModeRange(Mode.NORMAL, 5.0, 10.0),
            ModeRange(Mode.HIGH, 10.0, null),
        ),
    )

    private val analyzer = RecordingAnalyzer(rules)

    @Test
    fun `empty recording and single point give zero durations`() {
        for (samples in listOf(emptyList(), listOf(sample("0", 6.0, 100.0)))) {
            val totals = analyze(*samples.toTypedArray())
            assertSeconds("0", totals.spanS)
            assertSeconds("0", totals.workingS)
            assertSeconds("0", totals.unknownS)
        }
    }

    @Test
    fun `left sample describes the interval and the last sample adds no time`() {
        val totals = analyze(
            sample("0", 6.0, 100.0),
            sample("1", 0.0, 0.0),
            sample("2", 6.0, 100.0),
        )
        assertSeconds("1", totals.workingS)
        assertSeconds("1", totals.stoppedS)
        assertSeconds("2", totals.spanS)
    }

    @Test
    fun `interval of exactly 1_5 seconds is not a gap`() {
        val totals = analyze(sample("0", 6.0, 100.0), sample("1.5", 6.0, 100.0))
        assertSeconds("1.5", totals.workingS)
        assertSeconds("0", totals.gapS)
    }

    @Test
    fun `interval longer than 1_5 seconds is a gap even between working samples`() {
        val totals = analyze(sample("300", 6.0, 100.0), sample("1500", 6.0, 100.0))
        assertSeconds("1200", totals.gapS)
        assertSeconds("0", totals.workingS)
    }

    @Test
    fun `time differences are exact decimals`() {
        // в double 2.2 - 0.7 = 1.5000000000000002, был бы ложный пропуск
        val totals = analyze(sample("0", 6.0, 100.0), sample("0.7", 6.0, 100.0), sample("2.2", 6.0, 100.0))
        assertSeconds("2.2", totals.workingS)
        assertSeconds("0", totals.gapS)
    }

    @Test
    fun `gap wins over invalid left sample`() {
        val totals = analyze(sample("0", null, 100.0, valid = false), sample("5", 6.0, 100.0))
        assertSeconds("5", totals.gapS)
        assertSeconds("0", totals.invalidS)
    }

    @Test
    fun `invalid flag, nulls and negative values make the interval invalid`() {
        val totals = analyze(
            sample("0", 6.0, 100.0, valid = false),
            sample("1", null, 100.0),
            sample("2", 6.0, null),
            sample("3", -1.0, 100.0),
            sample("4", 6.0, -10.0),
            sample("5", 0.0, 0.0),
        )
        assertSeconds("5", totals.invalidS)
        assertSeconds("0", totals.workingS)
    }

    @Test
    fun `working requires both thresholds and mode boundaries are inclusive from below`() {
        val totals = analyze(
            sample("0", 0.999, 100.0), // stopped
            sample("1", 1.0, 49.999), // stopped
            sample("2", 1.0, 50.0), // low, ровно на порогах
            sample("3", 4.999, 100.0), // low
            sample("4", 5.0, 100.0), // normal
            sample("5", 9.999, 100.0), // normal
            sample("6", 10.0, 100.0), // high
            sample("7", 12.0, 0.0), // stopped, давление есть, оборотов нет
            sample("8", 0.0, 0.0),
        )
        assertSeconds("5", totals.workingS)
        assertSeconds("3", totals.stoppedS)
        assertSeconds("2", totals.modesS.getValue(Mode.LOW))
        assertSeconds("2", totals.modesS.getValue(Mode.NORMAL))
        assertSeconds("1", totals.modesS.getValue(Mode.HIGH))
    }

    @Test
    fun `span equals working plus stopped plus unknown`() {
        val totals = analyze(
            sample("0", 6.0, 100.0),
            sample("1", 0.0, 0.0),
            sample("2", 6.0, 100.0, valid = false),
            sample("3", 6.0, 100.0),
            sample("10", 2.0, 80.0),
            sample("11", 2.0, 80.0),
        )
        assertSeconds("11", totals.spanS)
        assertSeconds(totals.spanS.toPlainString(), totals.workingS + totals.stoppedS + totals.unknownS)
    }

    @Test
    fun `neighbouring intervals with the same state are merged into segments`() {
        val analysis = analyzer.analyze(
            Recording(
                "segments",
                listOf(
                    sample("0", 6.0, 100.0),
                    sample("1", 6.0, 100.0),
                    sample("2", 12.0, 100.0),
                    sample("3", 12.0, 100.0),
                    sample("10", 0.0, 0.0),
                ),
            ),
        )
        val segments = analysis.segments.map { Triple(it.start.toPlainString(), it.end.toPlainString(), it.state) }
        assertEquals(
            listOf(
                Triple("0", "2", IntervalState.Working(Mode.NORMAL)),
                Triple("2", "3", IntervalState.Working(Mode.HIGH)),
                Triple("3", "10", IntervalState.Gap),
            ),
            segments,
        )
    }

    private fun analyze(vararg samples: Sample): Totals = analyzer.analyze(Recording("test", samples.toList())).totals

    private fun sample(t: String, pressure: Double?, rpm: Double?, valid: Boolean = true) =
        Sample(BigDecimal(t), pressure, rpm, valid)

    private fun assertSeconds(expected: String, actual: BigDecimal) {
        assertEquals(0, BigDecimal(expected).compareTo(actual), "ожидалось $expected с, получено ${actual.toPlainString()} с")
    }
}
