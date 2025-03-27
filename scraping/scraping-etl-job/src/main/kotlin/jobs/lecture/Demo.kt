package io.gitp.ylfs.scraping.scraping_tl_job.jobs.lecture

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.college.CollegeEtlJob
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.dpt.DptRespTLJob
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.mileage.MileageInfo
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.mileage.MileageInfoRespExtractor
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.mileage.MileageLoader
import io.gitp.ylfs.scraping.scraping_tl_job.repositories.response.LectureProcessRepository
import io.gitp.ylfs.scraping.scraping_tl_job.repositories.response.LectureRespRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.transactions.transaction
import repositories.CollegeRepository
import repositories.DptRepository
import repositories.TermRepository
import repositories.response.CollegeRespRepository
import repositories.response.DptRespRepository
import java.sql.DriverManager
import java.time.Year
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

fun main() {
    val scrapingDB: Database =
        Database.connect(
            url = "jdbc:mysql://localhost/crawl",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "root",
            password = "root_pass",
            databaseConfig = DatabaseConfig { defaultReadOnly = true }
        )

    val db = HikariConfig()
        .apply {
            jdbcUrl = "jdbc:mysql://localhost/ylfs"
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
    val threadPool: ExecutorService = Executors.newFixedThreadPool(40)

    val collegeRespRepo = CollegeRespRepository(scrapingDB)
    val dptRespRepo = DptRespRepository(scrapingDB)
    val lectureRespRepo = LectureRespRepository(scrapingDB)

    val termRepo = TermRepository(db)
    val collegeRepo = CollegeRepository(db, termRepo)
    val dptRepo = DptRepository(db, termRepo, collegeRepo)
    val lectureParsedRepo = LectureProcessRepository(db)

    val collegeEtlJob = CollegeEtlJob(collegeRespRepo, termRepo, collegeRepo, threadPool)
    val dptRespTLJob = DptRespTLJob(dptRespRepo, dptRepo, threadPool)
    val lectureETLJob = LectureETLJob(lectureRespRepo, lectureParsedRepo, db, threadPool)

    val targetYear = Year.of(2024)
    val targetSemester = Semester.FIRST
    collegeEtlJob.execute(year = targetYear, semester = targetSemester)
    dptRespTLJob.execute(year = targetYear, semester = targetSemester)
    lectureETLJob.execute(year = targetYear, semester = targetSemester)

    val mileageInfoList: List<MileageInfo> = MileageInfoRespExtractor(scrapingDB).extract(year = targetYear, semester = targetSemester).filterNotNull().distinct()
    val conn = DriverManager.getConnection("jdbc:mysql://localhost/ylfs", "root", "root_pass")
    MileageLoader(conn).load(mileageInfoList, 40)


    threadPool.shutdownNow().also { require(it.size == 0) }
}
