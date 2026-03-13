// Pure Kotlin module — no Spring dependencies allowed

dependencies {
    api(project(":domain"))

    implementation(libs.slf4j.api)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.mockk)
}
