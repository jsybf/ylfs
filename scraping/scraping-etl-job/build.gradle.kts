plugins {
    id("kotlin-convention")
    alias(libs.plugins.kotlinx.serialization)
    id("application")
}

dependencies {
    implementation(project(":entity-module"))

    implementation(libs.logging.logback)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.mysql.connector.java)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.json)
    implementation(libs.hikari.cp)

    implementation(libs.clikt)

}

val mainClassRef = "io.gitp.ylfs.scraping.scraping_tl_job.jobs.lecture.DemoKt"

application {
    mainClass = mainClassRef
}

tasks.register<Jar>("fatJar") {
    group = "custom tasks"
    description = "Creates a self-contained fat JAR of the application that can be run."

    with(tasks.jar.get())

    manifest.attributes["Main-Class"] = mainClassRef
    archiveFileName = "${archiveBaseName.get()}-exec.jar"
    destinationDirectory = file("$rootDir/jars")

    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree)
    from(dependencies)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
