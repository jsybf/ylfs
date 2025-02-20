plugins {
    id("kotlin-convention")
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(project(":entity-module"))
    implementation(project(":entity-module"))
    implementation(libs.logging.slf4j)
}
