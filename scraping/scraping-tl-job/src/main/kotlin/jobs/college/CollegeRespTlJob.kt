package io.gitp.ylfs.scraping.scraping_tl_job.jobs.college

import io.gitp.ylfs.entity.enums.Semester
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import repositories.CollegeRepository
import repositories.TermRepository
import repositories.response.CollegeRespRepository
import java.time.Year
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

    fun execute() {
        val collegeResps: List<CollegeRespDto> = collegeRespRepo.findAll()

        val collegeDtos = collegeResps.flatMap { CollegeRespParser.toCollegeDtos(it) }
        val termDtos = collegeResps.map { TermDto(it.year, it.semester) }.distinct()

        termDtos
            .map { CompletableFuture.supplyAsync({ termRepo.insertIfNotExists(it) }, threadPool) }
            .map { it.join() }
        collegeDtos
            .map { CompletableFuture.supplyAsync({ collegeRepo.insertIfNotExists(it) }, threadPool) }
            .map { it.join() }
        threadPool.awaitTermination(1, TimeUnit.SECONDS)
        threadPool.shutdownNow().also { require(it.size == 0) }.onEach { println(it) }
        println("execute complete")
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
