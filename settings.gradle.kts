rootProject.name = "monitor-app"

include(
    // монитор насоса: расчёт, REST, сборка jar
    "pump-monitor",

    // страница монитора насоса: только статика в src/main/resources/static
    "web",
)
