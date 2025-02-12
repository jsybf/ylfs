plugins {
    id("kotlin-convention")
    alias(libs.plugins.kotlinx.serialization)
    id("application")
}

dependencies {
    implementation(project(":entity-module"))
    implementation(project(":crawl:ajax-crawl-core"))

    implementation(libs.logging.logback)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mysql.connector.java)
    implementation(libs.clikt)

}

application {
    mainClass = "io.gitp.ylfs.crawl.crawljob.MainKt"
}
