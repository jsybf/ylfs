plugins {
    kotlin("jvm") version "2.1.10"
}

group = "io.gitp.ysfl"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
