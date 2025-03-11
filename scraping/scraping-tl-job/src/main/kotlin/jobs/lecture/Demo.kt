package io.gitp.ylfs.scraping.scraping_tl_job.jobs.college

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.dpt.DptRespTLJob
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.lecture.LectureTLJob
import io.gitp.ylfs.scraping.scraping_tl_job.repositories.response.LectureRespRepository
import io.gitp.ylfs.scraping.scraping_tl_job.tables.LectureParsedRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.transactions.transaction
import repositories.CollegeRepository
import repositories.DptRepository
import repositories.TermRepository
import repositories.response.CollegeRespRepository
import repositories.response.DptRespRepository
import java.time.Year

fun main() {
    val scrapingDB: Database =
        Database.connect(
            url = "jdbc:mysql://3.38.191.11/crawl",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "root",
            password = "root_pass",
            databaseConfig = DatabaseConfig { defaultReadOnly = true }
        )

    val db = HikariConfig()
        .apply {
            jdbcUrl = "jdbc:mysql://3.38.191.11/ylfs"
            driverClassName = "com.mysql.cj.jdbc.Driver"
            username = "root"
            password = "root_pass"
            maximumPoolSize = 41
            isReadOnly = false
        }
        .let { Database.connect(datasource = HikariDataSource(it)) }

    transaction(db) {
        exec("SET FOREIGN_KEY_CHECKS = 0;")
        exec("truncate term;")
        exec("truncate college;")
        exec("truncate dpt;")
        exec("truncate lecture;")
        exec("truncate dpt_lecture;")
        exec("truncate loc_sched;")
        exec("truncate subclass;")
        exec("SET FOREIGN_KEY_CHECKS = 1;")
    }

    val collegeRespRepo = CollegeRespRepository(scrapingDB)
    val dptRespRepo = DptRespRepository(scrapingDB)
    val lectureRespRepo = LectureRespRepository(scrapingDB)

    val termRepo = TermRepository(db)
    val collegeRepo = CollegeRepository(db, termRepo)
    val dptRepo = DptRepository(db, termRepo, collegeRepo)
    val lectureRespProcessedRepository = LectureParsedRepository(db)

    val collegeRespTLJob = CollegeRespTlJob(collegeRespRepo, termRepo, collegeRepo)
    val dptRespTLJob = DptRespTLJob(dptRespRepo, dptRepo)
    val lectureTLJob = LectureTLJob(lectureRespRepo, lectureRespProcessedRepository, db)

    collegeRespTLJob.execute(year = Year.of(2023), semester = Semester.FIRST)
    dptRespTLJob.execute(year = Year.of(2023), semester = Semester.FIRST)
    lectureTLJob.execute(year = Year.of(2023), semester = Semester.FIRST)

    // lectureTLJob.executeBatch()


    println("foo")
}