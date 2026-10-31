import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.addAll("-Xjdk-release=17", "-Xjsr305=strict")
    }
}

dependencies {
    //internal
    //страница нужна только в рантайме: Spring отдаёт её из classpath:/static
    runtimeOnly(project(":web"))

    //kotlin
    implementation(rootProject.libs.kotlin.reflect)

    //spring
    implementation(rootProject.libs.spring.boot.starter.webmvc)

    //jackson
    implementation(rootProject.libs.tools.jackson.kotlin.v3)

    //tests
    testImplementation(rootProject.libs.test.spring.boot.starter.webmvc)
    testImplementation(rootProject.libs.test.kotlin.junit5)
    testImplementation(rootProject.libs.test.archunit)
    testRuntimeOnly(rootProject.libs.test.junit.platform)
}

tasks.bootRun {
    systemProperty("spring.profiles.active", "dev")
}

tasks.bootJar {
    archiveFileName = "pump-monitor.jar"
}

tasks.jar {
    enabled = false
}

tasks.test {
    useJUnitPlatform()
}
