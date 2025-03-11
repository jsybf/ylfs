package io.gitp.ylfs.scraping.scraping_tl_job.jobs.college

import io.gitp.ylfs.entity.enums.Semester
import kotlinx.serialization.json.JsonObject
import java.time.Year


data class TermDto(
    val year: Year,
    val semester: Semester
)

data class CollegeDto(
    val year: Year,
    val semester: Semester,
    val code: String,
    val name: String
)

data class CollegeRespDto(
    val year: Year,
    val semester: Semester,
    val resp: JsonObject
)
