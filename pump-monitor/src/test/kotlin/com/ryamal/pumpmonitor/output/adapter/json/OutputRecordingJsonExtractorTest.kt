package com.ryamal.pumpmonitor.output.adapter.json

import com.ryamal.pumpmonitor.core.component.FormatError
import com.ryamal.pumpmonitor.core.component.FormatErrorCategory
import com.ryamal.pumpmonitor.core.component.Recording
import com.ryamal.pumpmonitor.output.port.LoadResult
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class OutputRecordingJsonExtractorTest {

    private val extractor = OutputRecordingJsonExtractor()

    @Test
    fun `missing sample field is reported with its place`() {
        val error = rejected(file("""{"t_s": 0, "pressure_mpa": 6, "valid": true}"""))
        assertEquals(FormatErrorCategory.MISSING_FIELD, error.category)
        assertEquals("samples[0].rpm", error.path)
        assertEquals("В samples[0] отсутствует rpm.", error.message)
    }

    @Test
    fun `missing root field is reported`() {
        val error = rejected("""{"schema_version": "1.0", "synthetic": true, "samples": []}""")
        assertEquals(FormatErrorCategory.MISSING_FIELD, error.category)
        assertEquals("dataset_id", error.path)
    }

    @Test
    fun `string is not a number`() {
        val error = rejected(file("""{"t_s": 0, "pressure_mpa": "6", "rpm": 100, "valid": true}"""))
        assertEquals(FormatErrorCategory.WRONG_TYPE, error.category)
        assertEquals("samples[0].pressure_mpa", error.path)
        assertContains(error.message, "строка")
    }

    @Test
    fun `boolean is not a number`() {
        val error = rejected(file("""{"t_s": 0, "pressure_mpa": 6, "rpm": true, "valid": true}"""))
        assertEquals(FormatErrorCategory.WRONG_TYPE, error.category)
        assertEquals("samples[0].rpm", error.path)
    }

    @Test
    fun `time cannot be null or negative`() {
        assertEquals(
            FormatErrorCategory.WRONG_TYPE,
            rejected(file("""{"t_s": null, "pressure_mpa": 6, "rpm": 100, "valid": true}""")).category,
        )
        val negative = rejected(file("""{"t_s": -1, "pressure_mpa": 6, "rpm": 100, "valid": true}"""))
        assertEquals(FormatErrorCategory.INVALID_VALUE, negative.category)
        assertEquals("samples[0].t_s", negative.path)
    }

    @Test
    fun `valid must be a boolean`() {
        val error = rejected(file("""{"t_s": 0, "pressure_mpa": 6, "rpm": 100, "valid": "true"}"""))
        assertEquals(FormatErrorCategory.WRONG_TYPE, error.category)
        assertEquals("samples[0].valid", error.path)
    }

    @Test
    fun `repeated and decreasing time are rejected`() {
        val repeated = rejected(file(SAMPLE_AT_0, SAMPLE_AT_0))
        assertEquals(FormatErrorCategory.NON_INCREASING_TIME, repeated.category)
        assertEquals("samples[1].t_s", repeated.path)
        assertContains(repeated.message, "повторяет")

        val reversed = rejected(file(SAMPLE_AT_0, sampleAt("2"), sampleAt("1")))
        assertEquals(FormatErrorCategory.NON_INCREASING_TIME, reversed.category)
        assertEquals("samples[2].t_s", reversed.path)
    }

    @Test
    fun `0 and 0_0 are the same time`() {
        val error = rejected(file(SAMPLE_AT_0, sampleAt("0.0")))
        assertEquals(FormatErrorCategory.NON_INCREASING_TIME, error.category)
    }

    @Test
    fun `first error in document order wins`() {
        val error = rejected(
            file(SAMPLE_AT_0, SAMPLE_AT_0, """{"t_s": 2, "pressure_mpa": "6", "rpm": 100, "valid": true}"""),
        )
        assertEquals(FormatErrorCategory.NON_INCREASING_TIME, error.category)
    }

    @Test
    fun `only version 1_0 written as a string is supported`() {
        val other = rejected("""{"schema_version": "1.1", "dataset_id": "x", "synthetic": true, "samples": []}""")
        assertEquals(FormatErrorCategory.UNSUPPORTED_VERSION, other.category)

        val number = rejected("""{"schema_version": 1.0, "dataset_id": "x", "synthetic": true, "samples": []}""")
        assertEquals(FormatErrorCategory.WRONG_TYPE, number.category)
        assertEquals("schema_version", number.path)
    }

    @Test
    fun `synthetic must be boolean true`() {
        val falseValue = rejected("""{"schema_version": "1.0", "dataset_id": "x", "synthetic": false, "samples": []}""")
        assertEquals(FormatErrorCategory.INVALID_VALUE, falseValue.category)

        val string = rejected("""{"schema_version": "1.0", "dataset_id": "x", "synthetic": "true", "samples": []}""")
        assertEquals(FormatErrorCategory.WRONG_TYPE, string.category)
    }

    @Test
    fun `wrong structure is rejected`() {
        assertEquals(FormatErrorCategory.WRONG_TYPE, rejected("[]").category)

        val samplesObject = rejected("""{"schema_version": "1.0", "dataset_id": "x", "synthetic": true, "samples": {}}""")
        assertEquals(FormatErrorCategory.WRONG_TYPE, samplesObject.category)
        assertEquals("samples", samplesObject.path)

        val sampleArray = rejected(file("[0, 6, 100, true]"))
        assertEquals("samples[0]", sampleArray.path)
    }

    @Test
    fun `number too large for double is rejected`() {
        val error = rejected(file("""{"t_s": 0, "pressure_mpa": 6, "rpm": 1e400, "valid": true}"""))
        assertEquals(FormatErrorCategory.INVALID_VALUE, error.category)
        assertEquals("samples[0].rpm", error.path)
    }

    @Test
    fun `broken JSON, empty body, duplicate keys and trailing text are invalid JSON`() {
        val broken = listOf("{", "", """{"a": 1, "a": 2}""", """{"a": 1} {"b": 2}""")
        for (json in broken) {
            val error = rejected(json)
            assertEquals(FormatErrorCategory.INVALID_JSON, error.category, "для «$json»")
            assertNull(error.path)
        }
    }

    @Test
    fun `nulls in measurements and extra fields are accepted`() {
        val recording = loaded(
            """
            {
              "schema_version": "1.0", "dataset_id": "extra", "synthetic": true, "comment": "лишнее поле",
              "samples": [
                {"t_s": 0, "pressure_mpa": null, "rpm": 100.0, "valid": true, "temperature_c": 40},
                {"t_s": 0.7, "pressure_mpa": 6, "rpm": null, "valid": false}
              ]
            }
            """,
        )
        assertEquals("extra", recording.datasetId)
        assertNull(recording.samples[0].pressureMpa)
        assertNull(recording.samples[1].rpm)
        assertEquals(0, BigDecimal("0.7").compareTo(recording.samples[1].t))
    }

    private fun rejected(json: String): FormatError {
        val result = extractor.load(json)
        return assertIs<LoadResult.Rejected>(result).error
    }

    private fun loaded(json: String): Recording {
        val result = extractor.load(json)
        return assertIs<LoadResult.Loaded>(result).recording
    }

    private fun file(vararg samples: String): String =
        """{"schema_version": "1.0", "dataset_id": "test", "synthetic": true, "samples": [${samples.joinToString()}]}"""

    private fun sampleAt(t: String) = """{"t_s": $t, "pressure_mpa": 6, "rpm": 100, "valid": true}"""

    private companion object {
        val SAMPLE_AT_0 = """{"t_s": 0, "pressure_mpa": 6, "rpm": 100, "valid": true}"""
    }
}
