// Pure Kotlin module — no Spring dependencies allowed

dependencies {
    api(libs.arrow.core)
    implementation(libs.slf4j.api)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
}
