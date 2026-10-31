package com.ryamal.pumpmonitor.input.comm

import com.ryamal.pumpmonitor.core.component.FormatErrorCategory
import com.ryamal.pumpmonitor.core.operation.read.AnalysisOutcome
import com.ryamal.pumpmonitor.core.operation.read.AnalyzeRecordingOperation
import com.ryamal.pumpmonitor.input.comm.response.AnalysisResponse
import com.ryamal.pumpmonitor.input.comm.response.FormatErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AnalyzeController(private val analyzeRecording: AnalyzeRecordingOperation) {

    // тело берём строкой, без привязки к классам: формат проверяется в выходном адаптере
    @PostMapping("/api/analyze")
    fun analyze(@RequestBody(required = false) body: String?): ResponseEntity<Any> =
        when (val outcome = analyzeRecording.execute(body.orEmpty())) {
            is AnalysisOutcome.Analyzed -> ResponseEntity.ok(AnalysisResponse.from(outcome.analysis))
            is AnalysisOutcome.Rejected -> {
                val status = if (outcome.error.category == FormatErrorCategory.INVALID_JSON) {
                    HttpStatus.BAD_REQUEST
                } else {
                    HttpStatus.UNPROCESSABLE_CONTENT
                }
                ResponseEntity.status(status).body(FormatErrorResponse.from(outcome.error))
            }
        }
}
