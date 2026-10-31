package com.ryamal.pumpmonitor.core.component

enum class FormatErrorCategory(val id: String) {
    INVALID_JSON("invalid_json"),
    MISSING_FIELD("missing_field"),
    WRONG_TYPE("wrong_type"),
    INVALID_VALUE("invalid_value"),
    UNSUPPORTED_VERSION("unsupported_version"),
    NON_INCREASING_TIME("non_increasing_time"),
}

data class FormatError(
    val category: FormatErrorCategory,
    val path: String?, // например samples[0].rpm, null если ошибка про файл целиком
    val message: String,
)
