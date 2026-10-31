package com.ryamal.pumpmonitor.input.comm.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.ryamal.pumpmonitor.core.component.Analysis
import com.ryamal.pumpmonitor.core.component.Interval
import com.ryamal.pumpmonitor.core.component.IntervalState
import com.ryamal.pumpmonitor.core.component.Sample
import com.ryamal.pumpmonitor.core.component.Totals
import java.math.BigDecimal

data class AnalysisResponse(
    @get:JsonProperty("status") val status: String,
    @get:JsonProperty("dataset_id") val datasetId: String,
    @get:JsonProperty("sample_count") val sampleCount: Int,
    @get:JsonProperty("totals") val totals: TotalsDto,
    @get:JsonProperty("samples") val samples: List<SampleDto>,
    @get:JsonProperty("segments") val segments: List<SegmentDto>,
) {
    companion object {
        fun from(analysis: Analysis) = AnalysisResponse(
            status = "ok",
            datasetId = analysis.recording.datasetId,
            sampleCount = analysis.recording.samples.size,
            totals = TotalsDto.from(analysis.totals),
            samples = analysis.recording.samples.map(SampleDto::from),
            segments = analysis.segments.map(SegmentDto::from),
        )
    }
}

data class TotalsDto(
    @get:JsonProperty("span_s") val spanS: BigDecimal,
    @get:JsonProperty("working_s") val workingS: BigDecimal,
    @get:JsonProperty("stopped_s") val stoppedS: BigDecimal,
    @get:JsonProperty("gap_s") val gapS: BigDecimal,
    @get:JsonProperty("invalid_s") val invalidS: BigDecimal,
    @get:JsonProperty("unknown_s") val unknownS: BigDecimal,
    @get:JsonProperty("modes_s") val modesS: Map<String, BigDecimal>,
) {
    companion object {
        fun from(totals: Totals) = TotalsDto(
            spanS = totals.spanS.plain(),
            workingS = totals.workingS.plain(),
            stoppedS = totals.stoppedS.plain(),
            gapS = totals.gapS.plain(),
            invalidS = totals.invalidS.plain(),
            unknownS = totals.unknownS.plain(),
            modesS = totals.modesS.entries.associate { (mode, seconds) -> mode.id to seconds.plain() },
        )
    }
}

data class SampleDto(
    @get:JsonProperty("t_s") val tS: BigDecimal,
    @get:JsonProperty("pressure_mpa") val pressureMpa: Double?,
    @get:JsonProperty("rpm") val rpm: Double?,
    @get:JsonProperty("valid") val valid: Boolean,
) {
    companion object {
        fun from(sample: Sample) = SampleDto(sample.t.plain(), sample.pressureMpa, sample.rpm, sample.valid)
    }
}

data class SegmentDto(
    @get:JsonProperty("start_s") val startS: BigDecimal,
    @get:JsonProperty("end_s") val endS: BigDecimal,
    @get:JsonProperty("state") val state: String,
    @get:JsonProperty("mode") @get:JsonInclude(JsonInclude.Include.NON_NULL) val mode: String?,
) {
    companion object {
        fun from(interval: Interval): SegmentDto {
            val state = interval.state
            return SegmentDto(
                startS = interval.start.plain(),
                endS = interval.end.plain(),
                state = when (state) {
                    is IntervalState.Working -> "working"
                    IntervalState.Stopped -> "stopped"
                    IntervalState.Gap -> "gap"
                    IntervalState.Invalid -> "invalid"
                },
                mode = (state as? IntervalState.Working)?.mode?.id,
            )
        }
    }
}

// чтобы в json было 1130, а не 1130.00 или 1.13E+3
private fun BigDecimal.plain(): BigDecimal = stripTrailingZeros().let { if (it.scale() < 0) it.setScale(0) else it }
