package io.gitp.ylfs.scraping.scraping_tl_job.jobs.dpt

import io.gitp.ylfs.entity.enums.Semester
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import repositories.DptRepository
import repositories.response.DptRespRepository
import java.time.Year
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


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

private object DptRespParser {
    fun parse(dptResp: DptRespDto): List<DptDto> {
        val dptJsonArray = dptResp.resp.jsonObject["dsFaclyCd"]!!.jsonArray

        return dptJsonArray.map { dptJson ->
            DptDto(
                year = dptResp.year,
                semester = dptResp.semester,
                collegeCode = dptResp.collegeCode,
                code = dptJson.jsonObject["deptCd"]!!.jsonPrimitive.content,
                name = dptJson.jsonObject["deptNm"]!!.jsonPrimitive.content,
            )
        }
    }
}


/**
 * dpt_request테이블에서 읽어서 dpt테이블에 값 넣음
 *
 * dpt_group_request 테이블을 읽고 term, college 테이블에 값을 넣는
 * [CollegeRespTlJob]이 실행된걸 가정
 */
class DptRespTLJob(
    private val dptRespRepo: DptRespRepository,
    private val dptRepo: DptRepository,
    private val threadPool: ExecutorService = Executors.newFixedThreadPool(40)
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun execute() {
        val dpts: List<DptDto> = this.dptRespRepo
            .findAll()
            .flatMap { DptDto.parseResp(it) }

        dpts
            .map { dpt ->
                this.logger.debug("inserting {}", dpt)
                CompletableFuture.supplyAsync({ dptRepo.insertIfNotExists(dpt) }, threadPool)
            }
            .onEach { it.join() }

        threadPool.awaitTermination(1, TimeUnit.SECONDS)
        threadPool.shutdownNow().also { require(it.size == 0) }.onEach { println(it) }
    }
}
