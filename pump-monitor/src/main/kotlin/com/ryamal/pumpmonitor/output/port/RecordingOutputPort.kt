package com.ryamal.pumpmonitor.output.port

import com.ryamal.pumpmonitor.core.component.FormatError
import com.ryamal.pumpmonitor.core.component.Recording

fun interface RecordingOutputPort {
    fun load(content: String): LoadResult
}

sealed interface LoadResult {
    data class Loaded(val recording: Recording) : LoadResult

    data class Rejected(val error: FormatError) : LoadResult
}
