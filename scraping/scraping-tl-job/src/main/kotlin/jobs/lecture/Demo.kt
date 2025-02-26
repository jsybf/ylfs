package io.gitp.ylfs.scraping.scraping_tl_job.jobs.college

import io.gitp.ylfs.scraping.scraping_tl_job.jobs.dpt.DptRespTLJob
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.lecture.LectureTLJob
import io.gitp.ylfs.scraping.scraping_tl_job.repositories.DptLectureRepository
import io.gitp.ylfs.scraping.scraping_tl_job.repositories.LectureRepository
import io.gitp.ylfs.scraping.scraping_tl_job.repositories.SubClassRepository
import io.gitp.ylfs.scraping.scraping_tl_job.repositories.response.LectureRespRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.transactions.transaction
import repositories.CollegeRepository
import repositories.DptRepository
import repositories.TermRepository
import repositories.response.CollegeRespRepository
import repositories.response.DptRespRepository

fun main() {
    val scrapingDB: Database =
        Database.connect(
            url = "jdbc:mysql://52.78.137.40/crawl",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "root",
            password = "root_pass",
            databaseConfig = DatabaseConfig { defaultReadOnly = true }
        )
    val db: Database =
        Database.connect(
            url = "jdbc:mysql://52.78.137.40/ylfs",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "root",
            password = "root_pass"
        )

    transaction(db) {
        exec("SET FOREIGN_KEY_CHECKS = 0;")
        exec("truncate term;")
        exec("truncate college;")
        exec("truncate dpt;")
        exec("truncate lecture;")
        exec("truncate dpt_lecture;")
        exec("SET FOREIGN_KEY_CHECKS = 1;")
    }

    val collegeRespRepo = CollegeRespRepository(scrapingDB)
    val dptRespRepo = DptRespRepository(scrapingDB)
    val lectureRespRepo = LectureRespRepository(scrapingDB)

    val termRepo = TermRepository(db)
    val collegeRepo = CollegeRepository(db, termRepo)
    val dptRepo = DptRepository(db, termRepo, collegeRepo)
    val lectureRepo = LectureRepository(db)
    val dptLectureRepo = DptLectureRepository(db)
    val subClassRepo = SubClassRepository(db)

    val collegeRespTLJob = CollegeRespTlJob(collegeRespRepo, termRepo, collegeRepo)
    val dptRespTLJob = DptRespTLJob(dptRespRepo, dptRepo)
    val lectureTLJob = LectureTLJob(lectureRespRepo, lectureRepo, dptLectureRepo, subClassRepo)

    collegeRespTLJob.execute()
    dptRespTLJob.execute()
    lectureTLJob.execute()

    // lectureTLJob.executeBatch()


    println("foo")
}