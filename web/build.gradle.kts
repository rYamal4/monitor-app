plugins {
    `java-library`
}

// иначе модуль помечается версией JDK сборки, и pump-monitor (Java 17) его не подключит
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.processResources {
    exclude("static/mock/**")
}
