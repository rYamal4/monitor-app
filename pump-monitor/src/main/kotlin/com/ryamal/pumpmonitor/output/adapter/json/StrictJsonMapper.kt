package com.ryamal.pumpmonitor.output.adapter.json

import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper

// дробные числа читаем как BigDecimal; повторный ключ и мусор после json считаем ошибкой
internal val strictJsonMapper: JsonMapper = JsonMapper.builder()
    .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
    .build()
