package io.gitp.ylfs.scraping.scraping_tl_job.jobs.dpt

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.utils.supplyAsync
import org.slf4j.LoggerFactory
import repositories.DptRepository
import repositories.response.DptRespRepository
import java.time.Year
import java.util.concurrent.ExecutorService


class DptRespTLJob(
    private val dptRespRepo: DptRespRepository,
    private val dptRepo: DptRepository,
    private val threadPool: ExecutorService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun extract(year: Year, semester: Semester): List<DptRespDto> = dptRespRepo.findAll(year, semester)

    private fun transform(responses: List<DptRespDto>): List<DptDto> = responses.flatMap { DptDto.parseResp(it) }

    private fun load(dptDtos: List<DptDto>) =
        dptDtos
            .map { supplyAsync(threadPool) { dptRepo.insertIfNotExists(it) } }
            .onEach { it.join() }

    fun execute(year: Year, semester: Semester) {
        logger.info("extracting dpt_resp table")
        extract(year, semester)
            .also { logger.info("transforming department response") }
            .let { dptResps: List<DptRespDto> -> transform(dptResps) }
            .also { logger.info("loading department response") }
            .let { dptDtos: List<DptDto> -> load(dptDtos) }
    }
}
