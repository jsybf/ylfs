package io.gitp.ylfs.scraping.scraping_tl_job.jobs.lecture

import io.gitp.ylfs.entity.enums.GradeEvalMethod
import io.gitp.ylfs.entity.enums.Language
import io.gitp.ylfs.entity.enums.LectureType
import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.repositories.DptLectureRepository
import io.gitp.ylfs.scraping.scraping_tl_job.repositories.LectureRepository
import io.gitp.ylfs.scraping.scraping_tl_job.repositories.SubClassRepository
import io.gitp.ylfs.scraping.scraping_tl_job.repositories.response.LectureRespRepository
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Year
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class LectureRespDto(
    val year: Year,
    val semester: Semester,
    val collegeCode: String,
    val dptCode: String,
    val resp: JsonObject
)

data class LectureKey(
    val year: Year,
    val semester: Semester,
    val mainCode: String,
    val classCode: String,
    val id: Int?
)

data class LectureDto(
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
    val credit: BigDecimal,
    val gradeEvalMethod: GradeEvalMethod,
    val language: Language,

    val lectureType: LectureType
) {
    companion object {
        fun parse(resp: LectureRespDto): List<LectureDto> = LectureRespParser.parse(resp)
    }
}

object LectureRespParser {
    fun parse(lectureDto: LectureRespDto): List<LectureDto> {
        val lectureJsonArr = lectureDto.resp.jsonObject["dsSles251"]!!.jsonArray

        return lectureJsonArr.map { lectureJson ->
            println(lectureJson.jsonObject["cdt"]!!.jsonPrimitive.content)
            LectureDto(
                year = lectureDto.year,
                semester = lectureDto.semester,

                collegeCode = lectureDto.collegeCode,
                dptCode = lectureDto.dptCode,

                mainCode = lectureJson.jsonObject["subjtnb"]!!.jsonPrimitive.content,
                classCode = lectureJson.jsonObject["corseDvclsNo"]!!.jsonPrimitive.content,
                subCode = lectureJson.jsonObject["prctsCorseDvclsNo"]!!.jsonPrimitive.content,

                name = lectureJson.jsonObject["subjtNm"]!!.jsonPrimitive.content,
                professors = parseProfessors(lectureJson.jsonObject["cgprfNm"]!!.jsonPrimitive.content),

                grades = parseGrades(lectureJson.jsonObject["hy"]!!.jsonPrimitive.contentOrNull),

                credit = lectureJson.jsonObject["cdt"]!!.jsonPrimitive.content.let { BigDecimal(it) },
                gradeEvalMethod = parseGradeEvalMethod(lectureJson.jsonObject["gradeEvlMthdDivNm"]!!.jsonPrimitive.contentOrNull),
                language = parseLanguageCode(lectureJson.jsonObject["srclnLctreLangDivCd"]!!.jsonPrimitive.intOrNull),

                lectureType = LectureType.parse(lectureJson.jsonObject["subsrtDivNm"]!!.jsonPrimitive.contentOrNull)
            )
        }
    }

    fun parseProfessors(str: String?): List<String> {
        return if(str == null) emptyList()
        else str.split(",")
    }

    fun parseGrades(str: String?): List<Int> {
        return if (str == null) emptyList()
        else str.split(",").map { it.toInt() }.sorted()
    }

    fun parseGradeEvalMethod(str: String?): GradeEvalMethod = when (str) {
        null -> GradeEvalMethod.NONE
        "P/NP" -> GradeEvalMethod.P_OR_NP
        "절대평가" -> GradeEvalMethod.ABSOLUTE
        "상대평가" -> GradeEvalMethod.RELATIVE
        else -> throw IllegalStateException("unexpected grade eval method:${str}")
    }

    fun parseLanguageCode(code: Int?) = when (code) {
        null -> Language.KOREAN
        10 -> Language.ENGLISH
        20 -> Language.ETC
        else -> throw IllegalStateException("unexpected language code:${code}")
    }

}

fun main() {
    LectureRespParser.parseGrades(null).let { println(it) }
}

class LectureTLJob(
    private val lectureRespRepository: LectureRespRepository,
    private val lectureRepository: LectureRepository,
    private val dptLectureRepo: DptLectureRepository,
    private val subClassRepo: SubClassRepository,
    private val threadPool: ExecutorService = Executors.newFixedThreadPool(40)
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun execute() {
        val lectures = lectureRespRepository
            .findAll()
            .flatMap { resp -> LectureDto.parse(resp) }
            .slice(0..<100)

        lectures.map { lecture ->
            CompletableFuture.supplyAsync(
                {
                    lectureRepository.insertIfNotExists(lecture)
                    this.logger.debug("inserted{}", lecture)
                },
                threadPool
            )
        }.onEach { it.join() }

        lectures.map { lecture ->
            this.logger.debug("inserting {}", lecture)
            dptLectureRepo.insertIfNotExists(
                lecture.year,
                lecture.semester,
                lecture.mainCode,
                lecture.classCode,
                lecture.dptCode,
                lecture.lectureType
            )
        }

        lectures.map { lecture ->
            subClassRepo.insertIfNotExists(
                lecture.year,
                lecture.semester,
                lecture.mainCode,
                lecture.classCode,
                lecture.subCode
            )
        }


        threadPool.awaitTermination(1, TimeUnit.SECONDS)
        threadPool.shutdownNow().also { require(it.size == 0) }.onEach { println(it) }
    }
}