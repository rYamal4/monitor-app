package com.ryamal.pumpmonitor.output.adapter.json

import com.ryamal.pumpmonitor.core.component.Mode
import com.ryamal.pumpmonitor.core.component.ModeRange
import com.ryamal.pumpmonitor.core.component.Rules

object RulesLoader {

    fun fromClasspath(resource: String = "rules.json"): Rules {
        val stream = javaClass.classLoader.getResourceAsStream(resource) ?: error("Не найден ресурс $resource")
        return stream.use { parse(it.readAllBytes().decodeToString()) }
    }

    fun parse(json: String): Rules {
        val root = strictJsonMapper.readTree(json)
        val working = root.required("working_rule")
        check(working.required("operator").stringValue() == "AND") { "Поддерживается только working_rule.operator = AND" }

        val modes = root.required("modes_for_working_intervals").values().map { mode ->
            ModeRange(
                mode = Mode.byId(mode.required("id").stringValue()),
                pressureFrom = mode.required("pressure_mpa_gte").doubleValue(),
                pressureTo = mode.get("pressure_mpa_lt")?.doubleValue(),
            )
        }

        return Rules(
            maxIntervalS = root.required("max_interval_s").decimalValue(),
            rpmMin = working.required("rpm_gte").doubleValue(),
            pressureMin = working.required("pressure_mpa_gte").doubleValue(),
            modes = modes,
        )
    }
}
