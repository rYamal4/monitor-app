package com.ryamal.pumpmonitor.arch

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

@AnalyzeClasses(packages = ["com.ryamal.pumpmonitor"], importOptions = [ImportOption.DoNotIncludeTests::class])
class CheckLayerDependencies {

    // ядро знает только о выходных портах, но не об их реализациях, входе и Spring
    @ArchTest
    val coreDependsOnlyOnPorts: ArchRule = noClasses()
        .that().resideInAPackage("..pumpmonitor.core..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "org.springframework..",
            "tools.jackson..",
            "com.fasterxml.jackson..",
            "..pumpmonitor.input..",
            "..pumpmonitor.output.adapter..",
            "..pumpmonitor.config..",
        )

    @ArchTest
    val componentsDoNotDependOnOperationsOrPorts: ArchRule = noClasses()
        .that().resideInAPackage("..pumpmonitor.core.component..")
        .should().dependOnClassesThat().resideInAnyPackage("..pumpmonitor.core.operation..", "..pumpmonitor.output..")

    @ArchTest
    val inputDoesNotDependOnOutput: ArchRule = noClasses()
        .that().resideInAPackage("..pumpmonitor.input..")
        .should().dependOnClassesThat().resideInAPackage("..pumpmonitor.output..")

    @ArchTest
    val outputDoesNotDependOnInputOrOperations: ArchRule = noClasses()
        .that().resideInAPackage("..pumpmonitor.output..")
        .should().dependOnClassesThat().resideInAnyPackage("..pumpmonitor.input..", "..pumpmonitor.core.operation..")
}
