package com.ryamal.pumpmonitor.core.operation.read

import com.ryamal.pumpmonitor.core.component.Analysis
import com.ryamal.pumpmonitor.core.component.FormatError
import com.ryamal.pumpmonitor.output.port.LoadResult
import com.ryamal.pumpmonitor.output.port.RecordingOutputPort

class AnalyzeRecordingOperation(
    private val recordingOutputPort: RecordingOutputPort,
    private val analyzer: RecordingAnalyzer,
) {

    fun execute(content: String): AnalysisOutcome =
        when (val result = recordingOutputPort.load(content)) {
            is LoadResult.Loaded -> AnalysisOutcome.Analyzed(analyzer.analyze(result.recording))
            is LoadResult.Rejected -> AnalysisOutcome.Rejected(result.error)
        }
}

sealed interface AnalysisOutcome {
    data class Analyzed(val analysis: Analysis) : AnalysisOutcome

    data class Rejected(val error: FormatError) : AnalysisOutcome
}
