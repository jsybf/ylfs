plugins {
    id("kotlin-convention")
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":entity-module"))
}
