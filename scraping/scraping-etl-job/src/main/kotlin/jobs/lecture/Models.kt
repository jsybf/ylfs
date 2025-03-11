package io.gitp.ylfs.scraping.scraping_tl_job.jobs.lecture

import io.gitp.ylfs.entity.enums.GradeEvalMethod
import io.gitp.ylfs.entity.enums.Language
import io.gitp.ylfs.entity.enums.LectureType
import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.entity.model.LocAndSched
import io.gitp.ylfs.scraping.scraping_tl_job.BigDecimalSerailizer
import io.gitp.ylfs.scraping.scraping_tl_job.YearSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.math.BigDecimal
import java.time.Year


data class LectureRespDto(
    val year: Year,
    val semester: Semester,
    val collegeCode: String,
    val dptCode: String,
    val resp: JsonObject
)

@Serializable
data class LectureDto(
    @Serializable(with = YearSerializer::class)
    val year: Year,
    val semester: Semester,

    val collegeCode: String,
    val dptCode: String,

    val mainCode: String,
    val classCode: String,
    val subCode: String,

    val name: String,
    val professors: List<String>,

    val grades: List<Int>,
    @Serializable(with = BigDecimalSerailizer::class)
    val credit: BigDecimal,
    val gradeEvalMethod: GradeEvalMethod,
    val language: Language,

    val lectureType: LectureType,
    val locAndScheds: List<LocAndSched>
) {
    companion object {
        fun parse(resp: LectureRespDto): List<LectureDto> = LectureRespParser.parse(resp)
    }
}
