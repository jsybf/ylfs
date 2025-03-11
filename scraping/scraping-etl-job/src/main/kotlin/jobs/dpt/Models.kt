package io.gitp.ylfs.scraping.scraping_tl_job.jobs.dpt

import io.gitp.ylfs.entity.enums.Semester
import kotlinx.serialization.json.JsonObject
import java.time.Year


data class DptRespDto(
    val year: Year,
    val semester: Semester,
    val collegeCode: String,
    val resp: JsonObject
)

data class DptDto(
    val year: Year,
    val semester: Semester,
    val collegeCode: String,
    val code: String,
    val name: String
) {
    companion object {
        fun parseResp(dptResp: DptRespDto): List<DptDto> = DptRespParser.parse(dptResp)
    }
}
