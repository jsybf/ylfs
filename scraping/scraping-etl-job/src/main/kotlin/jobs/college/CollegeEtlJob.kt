package io.gitp.ylfs.scraping.scraping_tl_job.jobs.college

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.utils.supplyAsync
import org.slf4j.LoggerFactory
import repositories.CollegeRepository
import repositories.TermRepository
import repositories.response.CollegeRespRepository
import java.time.Year
import java.util.concurrent.ExecutorService


class CollegeEtlJob(
    private val collegeRespRepo: CollegeRespRepository,
    private val termRepo: TermRepository,
    private val collegeRepo: CollegeRepository,
    private val threadPool: ExecutorService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun extract(year: Year, semester: Semester): List<CollegeRespDto> = collegeRespRepo.findAll(year, semester)

    private fun transform(responses: List<CollegeRespDto>): List<CollegeDto> = responses.flatMap { CollegeRespParser.toCollegeDtos(it) }

    private fun load(collegeDtos: List<CollegeDto>) =
        collegeDtos
            .map { supplyAsync(threadPool) { collegeRepo.insertIfNotExists(it) } }
            .onEach { it.join() }


    fun execute(year: Year, semester: Semester) {
        termRepo.insertIfNotExists(TermDto(year, semester))

        logger.info("extracting college_resp table")
        extract(year, semester)
            .also { logger.info("transforming college response") }
            .let { collegeResps: List<CollegeRespDto> -> transform(collegeResps) }
            .also { logger.info("loading college response") }
            .let { colleges: List<CollegeDto> -> load(colleges) }
    }
}