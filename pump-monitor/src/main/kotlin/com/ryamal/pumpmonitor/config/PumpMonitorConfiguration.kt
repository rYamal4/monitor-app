package com.ryamal.pumpmonitor.config

import com.ryamal.pumpmonitor.core.component.Rules
import com.ryamal.pumpmonitor.core.operation.read.AnalyzeRecordingOperation
import com.ryamal.pumpmonitor.core.operation.read.RecordingAnalyzer
import com.ryamal.pumpmonitor.output.adapter.json.OutputRecordingJsonExtractor
import com.ryamal.pumpmonitor.output.adapter.json.RulesLoader
import com.ryamal.pumpmonitor.output.port.RecordingOutputPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PumpMonitorConfiguration {

    @Bean
    fun rules(): Rules = RulesLoader.fromClasspath()

    @Bean
    fun recordingOutputPort(): RecordingOutputPort = OutputRecordingJsonExtractor()

    @Bean
    fun analyzeRecordingOperation(recordingOutputPort: RecordingOutputPort, rules: Rules): AnalyzeRecordingOperation =
        AnalyzeRecordingOperation(recordingOutputPort, RecordingAnalyzer(rules))
}
