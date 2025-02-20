plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "ylfs"

include(":entity-module")
include(":yonsei-client-module")
include(":lecture-db-module")
include(":scraping:ajax-crawl-core")
include(":scraping:scraping-req-job")
include(":scraping:scraping-tl-job")
