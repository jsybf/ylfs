package io.gitp.ylfs.scraping.scraping_tl_job.repositories.response

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.lecture.LectureRespDto
import io.gitp.ylfs.scraping.scraping_tl_job.utils.execAndMap
import io.gitp.ylfs.scraping.scraping_tl_job.utils.getStringOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year

class LectureRespRepository(
    private val db: Database
) {
    private fun findAllQuery(year: Year, semester: Semester) = """
            SELECT lecture.college_code, lecture.dpt_code, lecture.http_resp_body, job.year, job.semester
            FROM lecture_resp as lecture
                JOIN crawl_job AS job USING(crawl_job_id)
           WHERE job.year = '${year}' and job.semester = '${semester.name}'
        """

    fun findAll(year: Year, semester: Semester): List<LectureRespDto> = transaction(db) {
        findAllQuery(year, semester)
            .execAndMap { rs ->
                val respJsonStr: String = rs.getStringOrNull("lecture.http_resp_body")!!
                val dptCode: String = rs.getStringOrNull("lecture.dpt_code")!!
                val collegeCode: String = rs.getStringOrNull("lecture.college_code")!!

                LectureRespDto(
                    year = year,
                    semester = semester,
                    collegeCode = collegeCode,
                    dptCode = dptCode,
                    resp = Json.decodeFromString<JsonObject>(respJsonStr)
                )
            }
    }
}