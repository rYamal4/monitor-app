package com.ryamal.pumpmonitor.input.comm.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.ryamal.pumpmonitor.core.component.FormatError

data class FormatErrorResponse(
    @get:JsonProperty("status") val status: String,
    @get:JsonProperty("category") val category: String,
    @get:JsonProperty("path") val path: String?,
    @get:JsonProperty("message") val message: String,
) {
    companion object {
        fun from(error: FormatError) = FormatErrorResponse(
            status = "format_error",
            category = error.category.id,
            path = error.path,
            message = error.message,
        )
    }
}
