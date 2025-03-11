package io.gitp.ylfs.scraping.scraping_tl_job.jobs.college

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.utils.supplyAsync
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import repositories.CollegeRepository
import repositories.TermRepository
import repositories.response.CollegeRespRepository
import java.time.Year
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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


class CollegeRespTlJob(
    private val collegeRespRepo: CollegeRespRepository,
    private val termRepo: TermRepository,
    private val collegeRepo: CollegeRepository,
    private val threadPool: ExecutorService = Executors.newFixedThreadPool(40)
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    fun execute(year: Year, semester: Semester) {
        val collegeResps: List<CollegeRespDto> = collegeRespRepo.findAll(year, semester)


        collegeResps
            .map { TermDto(it.year, it.semester) }
            .distinct()
            .map {
                supplyAsync(threadPool) {
                    termRepo.insertIfNotExists(it)
                    logger.debug("inserted term {}", it)
                }
            }.onEach { it.join() }

        collegeResps
            .flatMap { CollegeRespParser.toCollegeDtos(it) }
            .map {
                supplyAsync(threadPool) {
                    collegeRepo.insertIfNotExists(it)
                    logger.debug("inserted college {}", it)
                }
            }
            .onEach { it.join() }

        threadPool.shutdownNow().also { require(it.size == 0) }
    }
}

/**
 * internal for test
 */
object CollegeRespParser {
    fun toCollegeDtos(collegeRespDto: CollegeRespDto): List<CollegeDto> {
        val collegeJsonArray = collegeRespDto.resp.jsonObject["dsUnivCd"]!!.jsonArray

        return collegeJsonArray.map { collegeJson ->
            val collegeName = collegeJson.jsonObject["deptNm"]!!.jsonPrimitive.content
            val collegeCode = collegeJson.jsonObject["deptCd"]!!.jsonPrimitive.content

            CollegeDto(
                year = collegeRespDto.year,
                semester = collegeRespDto.semester,
                name = collegeName,
                code = collegeCode
            )
        }
    }

}
