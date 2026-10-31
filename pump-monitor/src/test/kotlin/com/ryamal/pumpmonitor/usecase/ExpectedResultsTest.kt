package com.ryamal.pumpmonitor.usecase

import com.ryamal.pumpmonitor.core.component.Mode
import com.ryamal.pumpmonitor.core.component.Totals
import com.ryamal.pumpmonitor.core.operation.read.AnalysisOutcome
import com.ryamal.pumpmonitor.core.operation.read.AnalyzeRecordingOperation
import com.ryamal.pumpmonitor.core.operation.read.RecordingAnalyzer
import com.ryamal.pumpmonitor.output.adapter.json.OutputRecordingJsonExtractor
import com.ryamal.pumpmonitor.output.adapter.json.RulesLoader
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertIs

// все файлы из resources/data и resources/data/extra против expected_results.json и expected_extra.json
class ExpectedResultsTest {

    private val analyzeRecording = AnalyzeRecordingOperation(OutputRecordingJsonExtractor(), RecordingAnalyzer(RulesLoader.fromClasspath()))
    private val mapper = JsonMapper.builder().build()

    @TestFactory
    fun `original data set`(): List<DynamicTest> = casesFrom("/data", "expected_results.json")

    @TestFactory
    fun `extra edge cases`(): List<DynamicTest> = casesFrom("/data/extra", "expected_extra.json")

    private fun casesFrom(dir: String, expectedFile: String): List<DynamicTest> {
        val results = mapper.readTree(resource("$dir/$expectedFile")).get("results")
        return results.propertyNames().map { fileName ->
            DynamicTest.dynamicTest(fileName) { check(resource("$dir/$fileName"), results.get(fileName)) }
        }
    }

    private fun resource(path: String): String = javaClass.getResource(path)?.readText() ?: error("Нет ресурса $path")

    private fun check(content: String, expected: JsonNode) {
        val outcome = analyzeRecording.execute(content)
        when (expected.get("status").stringValue()) {
            "ok" -> {
                val analysis = assertIs<AnalysisOutcome.Analyzed>(outcome, "ожидался успешный разбор").analysis
                assertEquals(expected.get("sample_count").intValue(), analysis.recording.samples.size, "sample_count")
                assertTotals(expected, analysis.totals)
            }
            "format_error" -> {
                val error = assertIs<AnalysisOutcome.Rejected>(outcome, "ожидалась ошибка формата").error
                assertEquals(expected.get("category").stringValue(), error.category.id, "category: ${error.message}")
                expected.get("path")?.let { assertEquals(it.stringValue(), error.path, "path") }
            }
            else -> error("Неизвестный статус в ожиданиях: ${expected.get("status")}")
        }
    }

    private fun assertTotals(expected: JsonNode, totals: Totals) {
        val actual = mapOf(
            "span_s" to totals.spanS,
            "working_s" to totals.workingS,
            "stopped_s" to totals.stoppedS,
            "gap_s" to totals.gapS,
            "invalid_s" to totals.invalidS,
            "unknown_s" to totals.unknownS,
        )
        for ((name, value) in actual) {
            assertSeconds(expected.get(name).decimalValue(), value, name)
        }
        for (mode in Mode.entries) {
            assertSeconds(expected.get("modes_s").get(mode.id).decimalValue(), totals.modesS.getValue(mode), "modes_s.${mode.id}")
        }
    }

    private fun assertSeconds(expected: BigDecimal, actual: BigDecimal, name: String) {
        assertEquals(0, expected.compareTo(actual), "$name: ожидалось ${expected.toPlainString()}, получено ${actual.toPlainString()}")
    }
}
