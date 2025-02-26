package io.gitp.ylfs.scraping.scraping_tl_job.jobs.college

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {
    val scrapingDB: Database =
        Database.connect(
            url = "jdbc:mysql://43.202.5.149:3306/crawl",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "root",
            password = "root_pass"
        )
    // val db =
    //     HikariConfig()
    //         .apply {
    //             this.jdbcUrl = "jdbc:mysql://43.202.5.149:3306/ylfs"
    //             this.username = "root"
    //             this.password = "root_pass"
    //             this.driverClassName = "com.mysql.cj.jdbc.Driver"
    //         }
    //         .let { Database.connect(HikariDataSource(it)) }


    val db: Database =
        Database.connect(
            url = "jdbc:mysql://43.202.5.149:3306/ylfs",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "root",
            password = "root_pass"
        )
    transaction(db) {
        exec("SET FOREIGN_KEY_CHECKS = 0;")
        exec("truncate term;")
        exec("truncate college;")
        exec("SET FOREIGN_KEY_CHECKS = 1;")
    }

    val job = CollegeRespTlJob(scrapingDB, db)
    job.execute()
    println("foo")
}