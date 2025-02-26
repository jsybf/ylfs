package io.gitp.ylfs.scraping.scraping_tl_job.repositories.response

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.dpt.DptRespDto
import io.gitp.ylfs.scraping.scraping_tl_job.utils.execAndMap
import io.gitp.ylfs.scraping.scraping_tl_job.utils.getStringOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year

class LecutreRespRepository(
    private val db: Database
) {
    private val findAllQuery = """
            SELECT 
            FROM lecture_resp as lecture
                JOIN crawl_job AS job USING(crawl_job_id)
        """

    fun findAll() = transaction(db) {
        findAllQuery.execAndMap { rs ->

            val yearStr: String = rs.getStringOrNull("job.year")!!
            val semesterStr: String = rs.getStringOrNull("job.semester")!!
            val collegeCode: String = rs.getStringOrNull("dpt.college_code")!!
            val respJsonStr: String = rs.getStringOrNull("dpt.http_resp_body")!!

            DptRespDto(
                year = yearStr.slice(0..3).let { s -> Year.parse(s) },
                semester = Semester.valueOf(semesterStr),
                collegeCode = collegeCode,
                resp = Json.decodeFromString<JsonObject>(respJsonStr)
            )
        }
    }
}