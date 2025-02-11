plugins {
    id("kotlin-convention")
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mysql.connector.java)
    implementation(project(":entity-module"))
    implementation(project(":crawl:ajax-crawl-core"))
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
}
