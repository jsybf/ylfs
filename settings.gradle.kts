plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "ylfs"

include(":entity-module")
include(":scraping:ajax-request-core")
include(":scraping:scraping-job")
