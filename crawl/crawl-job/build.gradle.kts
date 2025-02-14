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
    implementation(libs.hikari.cp)
    implementation(libs.clikt)

}

val mainClassRef = "io.gitp.ylfs.crawl.crawljob.MainKt"

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
