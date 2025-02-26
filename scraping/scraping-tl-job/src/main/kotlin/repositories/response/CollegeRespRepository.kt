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
    private fun findAllQuery(year: Year, semester: Semester) =
        """
           SELECT college.http_resp_body
           FROM college_resp as college
               JOIN crawl_job as job USING (crawl_job_id)
           WHERE job.year = '${year}' and job.semester = '${semester.name}'
        """

    fun findAll(year: Year, semester: Semester): List<CollegeRespDto> = transaction(db) {
        findAllQuery(year, semester).execAndMap {
            val respJsonStr: String = it.getStringOrNull("college.http_resp_body")!!

            CollegeRespDto(
                year = year,
                semester = semester,
                resp = Json.decodeFromString<JsonObject>(respJsonStr)
            )
        }
    }
}
