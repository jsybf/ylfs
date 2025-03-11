package io.gitp.ylfs.scraping.scraping_tl_job.jobs.lecture

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.repositories.response.LectureRespRepository
import io.gitp.ylfs.scraping.scraping_tl_job.tables.LectureParsedRepository
import io.gitp.ylfs.scraping.scraping_tl_job.utils.supplyAsync
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Year
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class LectureTLJob(
    private val lectureRespRepository: LectureRespRepository,
    private val lectureParsedRepository: LectureParsedRepository,
    private val db: Database
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun ingress(year: Year, semester: Semester): List<LectureDto> = lectureRespRepository
        .also { logger.info("loading lecture respond") }
        .findAll(year, semester)
        .flatMap { resp: LectureRespDto -> LectureDto.parse(resp) }

    fun egress(lectureDtos: List<LectureDto>) {
        logger.info("creating temporary processing table")
        transaction(db) { exec(lectureParsedTblDDL) }

        val threadPool: ExecutorService = Executors.newFixedThreadPool(40)
        logger.info("inserting parsed lecture response to temporary processing table")
        lectureDtos
            .chunked(256)
            .map { supplyAsync(threadPool) { lectureParsedRepository.batchInsert(it) } }
            .forEach { it.join() }

        threadPool.shutdownNow().also { require(it.size == 0) }

        logger.info("copy inserting from temporary processing table to lecture table")
        transaction(db) { exec(copyInsertLectureSql) }
        logger.info("copy inserting from temporary processing table to dpt_lecture table")
        transaction(db) { exec(copyInsertDptLectureSql) }
        logger.info("copy inserting from temporary processing table to subclass table")
        transaction(db) { exec(copyInsertSubclassSql) }
        logger.info("copy inserting from temporary processing table to sched_loc table")
        transaction(db) { exec(copyInsertLocSched) }

        logger.info("droping temporary processing table")
        transaction(db) { exec("drop table lecture_process_tbl") }
    }

    fun execute(year: Year, semester: Semester) {
        val lectureList = ingress(year, semester)
        egress(lectureList)
    }
}

private val lectureParsedTblDDL = """
                        CREATE TABLE IF NOT EXISTS lecture_process_tbl (
                            lecture_process_tbl_id INT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
                            year                      YEAR          NOT NULL,
                            semester                  VARCHAR(7)    NOT NULL,
                        
                            main_code                 CHAR(7)       NOT NULL,
                            class_code                CHAR(2)       NOT NULL,
                            sub_code                  CHAR(2)       NOT NULL,
                        
                            name                      VARCHAR(100)  NOT NULL,
                            professors                VARCHAR(255)  NOT NULL,
                        
                            grades                    VARCHAR(255)  NOT NULL,
                            credit                    DECIMAL(3, 1) NOT NULL,
                            grade_eval_method         VARCHAR(10)   NOT NULL,
                            language                  VARCHAR(10)   NOT NULL CHECK ( language IN ('KOREAN', 'ENGLISH', 'ETC') ),
                        
                            lecture_type              VARCHAR(10)    NOT NULL,
                            description               TEXT,
                            dpt_code                  VARCHAR(5)    NOT NULL,
                            loc_sched                 JSON          NOT NULL
                        );
        """

private val copyInsertLectureSql = """
                    INSERT INTO lecture(year, semester, main_code, class_code, name, professors, grades, credit, grade_eval_method, language, description)
                    WITH
                        lecture_partition AS (
                            SELECT *, ROW_NUMBER() OVER ( PARTITION BY main_code, class_code ORDER BY lecture_process_tbl_id) AS row_num
                            FROM lecture_process_tbl
                        ),
                        distinct_lectures AS (
                            SELECT *
                            FROM lecture_partition
                            WHERE row_num = 1
                        )
                    SELECT year, semester, main_code, class_code, name, professors, grades, credit, grade_eval_method, language, description
                    FROM distinct_lectures
                    ;
    """

private val copyInsertDptLectureSql = """
                INSERT INTO dpt_lecture(lecture_id, dpt_id, lecture_type)
                WITH
                    lectures AS (
                        SELECT lecture_process_tbl.*, lecture.lecture_id
                        FROM lecture_process_tbl
                             JOIN lecture USING (year, semester, main_code, class_code)
                    )
                SELECT lectures.lecture_id, dpt.dpt_id, lectures.lecture_type
                FROM lectures
                     JOIN term USING (year, semester)
                     JOIN college USING (term_id)
                     JOIN dpt ON lectures.dpt_code = dpt.code AND college.college_id = dpt.college_id
                ;
    """

private val copyInsertSubclassSql = """
INSERT INTO subclass(lecture_id, sub_id)
WITH
    lectures AS (
        SELECT lecture_process_tbl.*, lecture.lecture_id
        FROM lecture_process_tbl
             JOIN lecture USING (year, semester, main_code, class_code)
    )
SELECT DISTINCT lectures.lecture_id, lectures.sub_code
FROM lectures
;
    """

private val copyInsertLocSched = """
INSERT INTO loc_sched(subclass_id, loc_and_sched)
WITH
    lectures AS (
        SELECT lecture_process_tbl.*, subclass.subclass_id
        FROM lecture_process_tbl
             JOIN lecture USING (year, semester, main_code, class_code)
             JOIN subclass ON subclass.lecture_id = lecture.lecture_id AND subclass.sub_id = lecture_process_tbl.sub_code
    )
SELECT subclass_id, loc_sched
FROM lectures
;
    """
