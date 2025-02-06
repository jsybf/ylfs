plugins {
    id("kotlin-convention")
}

dependencies {
    implementation(project(":yonsei-client-module"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.mysql.connector.java)
}