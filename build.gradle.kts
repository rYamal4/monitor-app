plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

subprojects {
    group = "com.ryamal"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}
