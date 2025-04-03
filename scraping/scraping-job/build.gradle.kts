plugins {
    id("kotlin-convention")
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(project(":entity-module"))
    implementation(project(":scraping:ajax-request-core"))

    implementation(libs.clikt)
    implementation(libs.logging.logback)
    implementation(libs.duckdb)
    implementation(libs.mysql.connector.java)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

}

val mainClassRef = "io.gitp.yfls.scarping.job.file.MainKt"

tasks.register<Jar>("fatJar") {
    group = "custom tasks"
    description = "Creates a self-contained fat JAR of the application that can be run."

    with(tasks.jar.get())

    manifest.attributes["Main-Class"] = mainClassRef
    archiveFileName = "${archiveBaseName.get()}.jar"
    destinationDirectory = file("$rootDir/jars")

    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree)
    from(dependencies)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
