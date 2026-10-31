package com.ryamal.pumpmonitor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PumpMonitorApplication

fun main(args: Array<String>) {
    runApplication<PumpMonitorApplication>(*args)
}
