package com.ryamal.pumpmonitor.input.comm

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertContains

@SpringBootTest
@AutoConfigureMockMvc
class AnalyzeControllerTest(@param:Autowired private val mockMvc: MockMvc) {

    @Test
    fun `valid file returns totals, samples and segments`() {
        analyze("01_small.json").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ok") }
            jsonPath("$.dataset_id") { value("01_small") }
            jsonPath("$.sample_count") { value(13) }
            jsonPath("$.totals.working_s") { value(9) }
            jsonPath("$.totals.stopped_s") { value(3) }
            jsonPath("$.totals.unknown_s") { value(0) }
            jsonPath("$.totals.modes_s.low") { value(3) }
            jsonPath("$.samples.length()") { value(13) }
            jsonPath("$.segments[0].state") { value("stopped") }
            jsonPath("$.segments[1].state") { value("working") }
            jsonPath("$.segments[1].mode") { value("low") }
        }
    }

    @Test
    fun `gap is returned as a segment`() {
        analyze("03_gap_20min.json").andExpect {
            status { isOk() }
            jsonPath("$.totals.gap_s") { value(1200) }
            jsonPath("$.segments[1].state") { value("gap") }
            jsonPath("$.segments[1].start_s") { value(300) }
            jsonPath("$.segments[1].end_s") { value(1500) }
        }
    }

    @Test
    fun `format error returns 422 with place and no totals`() {
        analyze("08_missing_rpm.json").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.status") { value("format_error") }
            jsonPath("$.category") { value("missing_field") }
            jsonPath("$.path") { value("samples[0].rpm") }
            jsonPath("$.totals") { doesNotExist() }
        }
    }

    @Test
    fun `body that is not JSON returns 400`() {
        mockMvc.post("/api/analyze") {
            contentType = MediaType.APPLICATION_JSON
            content = "not json"
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.category") { value("invalid_json") }
        }
    }

    @Test
    fun `empty body returns 400`() {
        mockMvc.post("/api/analyze") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.category") { value("invalid_json") }
        }
    }

    @Test
    fun `page and charts library are served from the jar resources`() {
        val page = mockMvc.get("/index.html").andExpect { status { isOk() } }.andReturn()
        // charset в заголовке нет, браузер берёт его из <meta>, поэтому читаем как utf-8
        assertContains(page.response.getContentAsString(Charsets.UTF_8), "Контроль насоса")
        mockMvc.get("/vendor/echarts.min.js").andExpect { status { isOk() } }
    }

    private fun analyze(fileName: String) =
        mockMvc.post("/api/analyze") {
            contentType = MediaType.APPLICATION_JSON
            content = javaClass.getResource("/data/$fileName")!!.readText()
        }
}
