package io.gitp.ylfs.scraping.scraping_req_job

import io.gitp.ylfs.crawl.client.*
import io.gitp.ylfs.crawl.payload.*
import io.gitp.ylfs.entity.type.LectureId
import io.gitp.ylfs.entity.type.Semester
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.time.Year
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger


/**
 * request and persist to db
 * code is little bit verbose because hierarchical structure of college, department, lecture, mileage
 *
 * since relation between these data is one to many. number of mileage increase by n^3. which means
 * mileage requesting and persisting code will be buttleneck. so i wrote mileage relatd code condsidering
 * performance.
 *
 */
internal class CrawlJob(
    private val crawlJobRepo: CrawlJobRepository,
    private val year: Year,
    private val semester: Semester,
    private val depth: Int,
    private val chunkSize: Int = 64
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val extractCollegeId = Regex(""" "deptCd":"(?<dptId>\w+)" """, RegexOption.COMMENTS)
    private val extractDptId = Regex(""" "deptCd":"(?<dptId>\d+)" """, RegexOption.COMMENTS)
    private val extractCourseId = Regex(""" "subjtnbCorsePrcts":"([\dA-Z]{7})-(\d{2})-(\d{2})" """, RegexOption.COMMENTS)

    private fun requestCollege(): CollegeResp {
        return CollegeClient
            .request(CollegePayload(year, semester))
            .get()
            .getOrNull()!!
            .let { CollegeResp(it) }
    }

    private fun requestDpts(dptReqIds: List<DptReqId>): List<DptResp> {
        return dptReqIds
            .map { dptRequestId ->
                DptClient
                    .request(DptPayload(dptRequestId.collegeId, year, semester))
                    .thenApply { responseResult: Result<String> ->
                        DptResp(dptRequestId, responseResult.getOrNull()!!)
                    }
            }
            .map { it.get() }
    }

    private fun requestLectures(lectureReqIds: List<LectureReqId>): List<LectureResp> {
        return lectureReqIds
            .map { lectureReqId: LectureReqId ->
                LectureClient
                    .request(LecturePayload(lectureReqId.collegeId, lectureReqId.dptId, year, semester))
                    .thenApply { respResult: Result<String> ->
                        LectureResp(lectureReqId, respResult.getOrNull()!!)
                    }
            }
            .map { it.get() }
    }

    private fun requestMlgRanks(mlgReqIds: List<MlgReqId>): List<CompletableFuture<MlgRankResp>> {
        return mlgReqIds
            .map { reqId ->
                MlgRankClient
                    .request(MlgRankPayload(reqId.lectureId, year, semester))
                    .thenApplyAsync { responseResult: Result<String> ->
                        MlgRankResp(reqId, responseResult.getOrNull()!!)
                    }
            }
    }

    private fun requestMlgInfo(mlgReqIds: List<MlgReqId>): List<CompletableFuture<MlgInfoResp>> {
        return mlgReqIds
            .map { reqId ->
                MlgInfoClient
                    .request(MlgInfoPayload(reqId.lectureId, year, semester))
                    .thenApplyAsync { responseResult: Result<String> ->
                        MlgInfoResp(reqId, responseResult.getOrNull()!!)
                    }
            }
    }

    private fun parseDptReqId(collegeResp: CollegeResp): List<DptReqId> {
        return extractCollegeId
            .findAll(collegeResp.resp)
            .map { matchResult: MatchResult ->
                DptReqId(matchResult.destructured.component1())
            }
            .toList()
    }


    private fun parseLectureReqId(dptResps: List<DptResp>): List<LectureReqId> {
        val lectureReqIds: List<LectureReqId> = dptResps
            .flatMap { (dptReqId, resp) ->
                extractDptId
                    .findAll(resp)
                    .map { matchResult -> LectureReqId(dptReqId.collegeId, matchResult.destructured.component1()) }
            }

        return lectureReqIds
    }

    private fun parseMlgReqIds(lectureResps: List<LectureResp>): List<MlgReqId> {
        val mlgReqIds: List<MlgReqId> = lectureResps
            .flatMap { (lectureReqId, response) ->
                extractCourseId
                    .findAll(response)
                    .map { matchResult: MatchResult ->
                        val lectureId = LectureId(
                            mainId = matchResult.destructured.component1(),
                            classId = matchResult.destructured.component2(),
                            subId = matchResult.destructured.component3()
                        )
                        MlgReqId(lectureId)
                    }
            }
        return mlgReqIds
    }


    internal fun execute() {
        this.crawlJobRepo.startJob()

        /* crawl college */
        logger.info("requesting college")
        val collegeResp = requestCollege()
        logger.info("persisting college")
        crawlJobRepo.insertCollegeResp(collegeResp)

        if (this.depth == 1) {
            this.crawlJobRepo.endJob()
            return
        }


        /* crawl department */
        val dptReqIds = parseDptReqId(collegeResp)

        logger.info("requesting department")
        val dptResps = requestDpts(dptReqIds)
        logger.info("persisting department")
        crawlJobRepo.batchInsertDptResps(dptResps)

        if (this.depth == 2) {
            this.crawlJobRepo.endJob()
            return
        }

        /* crawl lecture */
        val lectureReqIds = parseLectureReqId(dptResps)

        logger.info("requesting lecture")
        val lectureResps = requestLectures(lectureReqIds)
        logger.info("persisting lecture")
        crawlJobRepo.batchInsertLectureResps(lectureResps)

        if (this.depth == 3) {
            this.crawlJobRepo.endJob()
            return
        }

        /* crawl mileage rank and milage info */
        val mlgReqIds = parseMlgReqIds(lectureResps)

        val totalMlgRankReqCnt = mlgReqIds.size
        var mlgRankReqCnt = AtomicInteger()
        val startTime = LocalDateTime.now()
        fun duration() = Duration.between(startTime, LocalDateTime.now())

        logger.info("requesting and persisting mileage rank")
        requestMlgRanks(mlgReqIds)
            .chunked(chunkSize)
            .map { respFutures: List<CompletableFuture<MlgRankResp>> ->
                CompletableFuture.runAsync {
                    val resps = respFutures.map { it.get() }
                    crawlJobRepo.batchInsertMlgRankResps(resps)
                    logger.debug(
                        "persisting mileage rank chunk. progress: [{}/{}] duration: [{}s]",
                        mlgRankReqCnt.addAndGet(resps.size), totalMlgRankReqCnt, duration().toSeconds()
                    )
                }
            }
            .forEach { it.join() }

        mlgRankReqCnt = AtomicInteger()

        logger.info("requesting and persisting mileage info")
        requestMlgInfo(mlgReqIds)
            .chunked(chunkSize)
            .map { respFutures: List<CompletableFuture<MlgInfoResp>> ->
                CompletableFuture.runAsync {
                    val resps = respFutures.map { it.get() }
                    crawlJobRepo.batchInsertMlgInfoResps(resps)
                    logger.debug(
                        "persisting mileage info chunk. progress: [{}/{}] duration: [{}s]",
                        mlgRankReqCnt.addAndGet(resps.size), totalMlgRankReqCnt, duration().toSeconds()
                    )
                }
            }
            .forEach { it.join() }

        this.crawlJobRepo.endJob()
    }


}
