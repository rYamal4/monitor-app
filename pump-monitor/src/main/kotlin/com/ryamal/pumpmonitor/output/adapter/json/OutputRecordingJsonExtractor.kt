package com.ryamal.pumpmonitor.output.adapter.json

import com.ryamal.pumpmonitor.core.component.FormatError
import com.ryamal.pumpmonitor.core.component.FormatErrorCategory
import com.ryamal.pumpmonitor.core.component.FormatErrorCategory.INVALID_JSON
import com.ryamal.pumpmonitor.core.component.FormatErrorCategory.INVALID_VALUE
import com.ryamal.pumpmonitor.core.component.FormatErrorCategory.MISSING_FIELD
import com.ryamal.pumpmonitor.core.component.FormatErrorCategory.NON_INCREASING_TIME
import com.ryamal.pumpmonitor.core.component.FormatErrorCategory.UNSUPPORTED_VERSION
import com.ryamal.pumpmonitor.core.component.FormatErrorCategory.WRONG_TYPE
import com.ryamal.pumpmonitor.core.component.Recording
import com.ryamal.pumpmonitor.core.component.Sample
import com.ryamal.pumpmonitor.output.port.LoadResult
import com.ryamal.pumpmonitor.output.port.RecordingOutputPort
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import java.math.BigDecimal

// Типы проверяем сами по дереву json: обычная привязка Jackson молча превращает "6" в число.
// Останавливаемся на первой ошибке.
class OutputRecordingJsonExtractor : RecordingOutputPort {

    override fun load(content: String): LoadResult =
        try {
            LoadResult.Loaded(parse(readTree(content)))
        } catch (violation: FormatViolation) {
            LoadResult.Rejected(violation.error)
        }

    private fun readTree(content: String): JsonNode =
        try {
            strictJsonMapper.readTree(content)
        } catch (e: JacksonException) {
            val location = e.location?.let { " (строка ${it.lineNr}, столбец ${it.columnNr})" }.orEmpty()
            fail(INVALID_JSON, null, "Файл не является корректным JSON$location: ${e.originalMessage}")
        }

    private fun parse(root: JsonNode): Recording {
        if (root.isMissingNode) fail(INVALID_JSON, null, "Файл пуст.")
        if (!root.isObject) fail(WRONG_TYPE, null, "Корень файла должен быть объектом, а ${written(root)}.")

        val version = field(root, "schema_version", null)
        if (!version.isString) fail(WRONG_TYPE, "schema_version", "В schema_version ${written(version)} вместо строки.")
        if (version.stringValue() != SUPPORTED_VERSION) {
            fail(
                UNSUPPORTED_VERSION,
                "schema_version",
                "Версия формата «${version.stringValue()}» не поддерживается, ожидается «$SUPPORTED_VERSION».",
            )
        }

        val datasetId = field(root, "dataset_id", null)
        if (!datasetId.isString) fail(WRONG_TYPE, "dataset_id", "В dataset_id ${written(datasetId)} вместо строки.")

        val synthetic = field(root, "synthetic", null)
        if (!synthetic.isBoolean) {
            fail(WRONG_TYPE, "synthetic", "В synthetic ${written(synthetic)} вместо логического значения.")
        }
        if (!synthetic.booleanValue()) {
            fail(INVALID_VALUE, "synthetic", "synthetic должен быть true: прототип работает только с учебными записями.")
        }

        val samplesNode = field(root, "samples", null)
        if (!samplesNode.isArray) fail(WRONG_TYPE, "samples", "samples должен быть массивом, а ${written(samplesNode)}.")

        val samples = ArrayList<Sample>(samplesNode.size())
        for (i in 0 until samplesNode.size()) {
            val sample = parseSample(samplesNode.get(i), "samples[$i]")
            samples.lastOrNull()?.let { checkOrder(it, sample, "samples[$i].t_s") }
            samples += sample
        }
        return Recording(datasetId.stringValue(), samples)
    }

    private fun parseSample(node: JsonNode, path: String): Sample {
        if (!node.isObject) fail(WRONG_TYPE, path, "$path должен быть объектом, а ${written(node)}.")

        val t = number(field(node, "t_s", path), "$path.t_s")
            ?: fail(WRONG_TYPE, "$path.t_s", "В $path.t_s записан null вместо числа.")
        if (t.signum() < 0) fail(INVALID_VALUE, "$path.t_s", "$path.t_s не может быть отрицательным: ${t.toPlainString()}.")

        val pressure = number(field(node, "pressure_mpa", path), "$path.pressure_mpa")
        val rpm = number(field(node, "rpm", path), "$path.rpm")

        val valid = field(node, "valid", path)
        if (!valid.isBoolean) fail(WRONG_TYPE, "$path.valid", "В $path.valid ${written(valid)} вместо логического значения.")

        return Sample(t, pressure?.toDouble(), rpm?.toDouble(), valid.booleanValue())
    }

    private fun checkOrder(previous: Sample, current: Sample, path: String) {
        val order = current.t.compareTo(previous.t)
        if (order == 0) {
            fail(NON_INCREASING_TIME, path, "$path повторяет предыдущую метку времени (${current.t.toPlainString()}).")
        }
        if (order < 0) {
            fail(
                NON_INCREASING_TIME,
                path,
                "$path (${current.t.toPlainString()}) меньше предыдущей метки (${previous.t.toPlainString()}): " +
                    "время идёт в обратном порядке.",
            )
        }
    }

    private fun number(node: JsonNode, path: String): BigDecimal? {
        if (node.isNull) return null
        if (!node.isNumber) fail(WRONG_TYPE, path, "В $path ${written(node)} вместо числа.")
        val value = node.decimalValue()
        if (!value.toDouble().isFinite()) fail(INVALID_VALUE, path, "В $path слишком большое число: ${node.asString()}.")
        return value
    }

    private fun field(node: JsonNode, name: String, parentPath: String?): JsonNode =
        node.get(name) ?: if (parentPath == null) {
            fail(MISSING_FIELD, name, "В файле отсутствует поле $name.")
        } else {
            fail(MISSING_FIELD, "$parentPath.$name", "В $parentPath отсутствует $name.")
        }

    private fun written(node: JsonNode): String =
        when {
            node.isString -> "записана строка «${node.stringValue()}»"
            node.isBoolean -> "записано логическое значение ${node.booleanValue()}"
            node.isNull -> "записан null"
            node.isNumber -> "записано число ${node.asString()}"
            node.isObject -> "записан объект"
            node.isArray -> "записан массив"
            else -> "записано значение неподдерживаемого типа"
        }

    private fun fail(category: FormatErrorCategory, path: String?, message: String): Nothing =
        throw FormatViolation(FormatError(category, path, message))

    private class FormatViolation(val error: FormatError) : RuntimeException(error.message)

    private companion object {
        const val SUPPORTED_VERSION = "1.0"
    }
}
