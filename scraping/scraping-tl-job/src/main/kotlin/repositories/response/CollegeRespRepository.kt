package repositories.response

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.college.CollegeRespDto
import io.gitp.ylfs.scraping.scraping_tl_job.utils.execAndMap
import io.gitp.ylfs.scraping.scraping_tl_job.utils.getStringOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year

class CollegeRespRepository(
    private val db: Database
) {
    private val findAllQuery =
        """
           SELECT job.year, job.semester, college.http_resp_body
           FROM college_resp as college
               JOIN crawl_job as job USING (crawl_job_id)
        """

    fun findAll(): List<CollegeRespDto> = transaction(db) {
        findAllQuery.execAndMap {
            val yearStr: String = it.getStringOrNull("job.year")!!
            val semesterStr: String = it.getStringOrNull("job.semester")!!
            val respJsonStr: String = it.getStringOrNull("college.http_resp_body")!!

            CollegeRespDto(
                year = yearStr.slice(0..3).let { s -> Year.parse(s) },
                semester = Semester.valueOf(semesterStr),
                resp = Json.decodeFromString<JsonObject>(respJsonStr)
            )
        }
    }
}
