pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "clean-architecture-sdd"

include(
    "domain",
    "application",
    "infrastructure",
    "presentation",
    "framework",
    "detekt-rules",
)
