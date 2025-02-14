package io.gitp.ylfs.crawl.crawljob

import io.gitp.ylfs.crawl.client.CollegeClient
import io.gitp.ylfs.crawl.client.DptClient
import io.gitp.ylfs.crawl.client.LectureClient
import io.gitp.ylfs.crawl.client.MlgRankClient
import io.gitp.ylfs.crawl.payload.CollegePayload
import io.gitp.ylfs.crawl.payload.DptPayload
import io.gitp.ylfs.crawl.payload.LecturePayload
import io.gitp.ylfs.crawl.payload.MlgRankPayload
import io.gitp.ylfs.entity.type.LectureId
import io.gitp.ylfs.entity.type.Semester
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.time.Year
import java.util.concurrent.atomic.AtomicInteger


private val extractCollegeId = Regex(""" "deptCd":"(?<dptId>\w+)" """, RegexOption.COMMENTS)
private val extractDptId = Regex(""" "deptCd":"(?<dptId>\d+)" """, RegexOption.COMMENTS)
private val extractCourseId = Regex(""" "subjtnbCorsePrcts":"([\dA-Z]{7})-(\d{2})-(\d{2})" """, RegexOption.COMMENTS)

private val logger = LoggerFactory.getLogger(object {}::class.java.`package`.name)

/**
 * request to yonsei course search server and
 * persist raw json http response body to mysql
 */
internal fun crawlJob(
    mysqlUsername: String,
    mysqlPassword: String,
    mysqlHost: String,
    mysqlDatabase: String,
    year: Year,
    semester: Semester,
    requestDepth: Int
) {
    require(requestDepth in (1..4))

    val repo = RawJsonRespRepository(mysqlHost, mysqlDatabase, mysqlUsername, mysqlPassword, year, semester)
    repo.startJob()

    /* request college */
    logger.info("requesting college data")
    val collegeResp: CollegeResp = CollegeClient
        .request(CollegePayload(year, semester))
        .get()
        .getOrNull()!!
        .let { CollegeResp(it) }

    logger.info("inserting college response to db.")
    repo.insertCollegeResp(collegeResp)


    if (requestDepth == 1) {
        repo.endJob()
        return
    }


    /* request Dpt */
    val dptReqIds: List<DptReqId> =
        extractCollegeId
            .findAll(collegeResp.resp)
            .map { matchResult: MatchResult ->
                DptReqId(matchResult.destructured.component1())
            }
            .toList()

    logger.info("requesting department data")
    val dptResps: List<DptResp> = dptReqIds
        .map { dptRequestId ->
            DptClient
                .request(DptPayload(dptRequestId.collegeId, year, semester))
                .thenApply { responseResult: Result<String> ->
                    DptResp(dptRequestId, responseResult.getOrNull()!!)
                }
        }
        .map { it.get() }

    logger.info("inserting department response to db.")
    repo.batchInsertDptResps(dptResps)

    if (requestDepth == 2) {
        repo.endJob()
        return
    }

    /* request lecture */
    val lectureReqIds: List<LectureReqId> = dptResps
        .flatMap { (dptReqId, resp) ->
            extractDptId
                .findAll(resp)
                .map { matchResult -> LectureReqId(dptReqId.collegeId, matchResult.destructured.component1()) }
        }


    logger.info("requesting lecture data")
    val lectureResps: List<LectureResp> = lectureReqIds
        .map { lectureReqId: LectureReqId ->
            LectureClient
                .request(LecturePayload(lectureReqId.collegeId, lectureReqId.dptId, year, semester))
                .thenApply { respResult: Result<String> ->
                    LectureResp(lectureReqId, respResult.getOrNull()!!)
                }
        }
        .map { it.get() }

    logger.info("inserting lecture response to db.")
    repo.batchInsertLectureResps(lectureResps)

    if (requestDepth == 3) {
        repo.endJob()
        return
    }

    /* reqeust Mileages */
    val mlgRankReqIds: List<MlgRankReqId> = lectureResps
        .flatMap { (courseRequestId, response) ->
            extractCourseId
                .findAll(response)
                .map { matchResult: MatchResult ->
                    val lectureId = LectureId(
                        mainId = matchResult.destructured.component1(),
                        classId = matchResult.destructured.component2(),
                        subId = matchResult.destructured.component3()
                    )
                    MlgRankReqId(lectureId)
                }
        }

    val totalMlgRankReqCnt = mlgRankReqIds.size
    var mlgRankReqCnt = AtomicInteger()
    val chunkSize = 64
    val startTime = LocalDateTime.now()
    fun duration() = Duration.between(startTime, LocalDateTime.now())

    logger.info("requesting mileage rank data and inserting mileage data chunk")

    mlgRankReqIds
        .chunked(chunkSize)
        .map { reqIds ->
            reqIds.map { reqId ->
                MlgRankClient
                    .request(MlgRankPayload(reqId.lectureId, year, semester))
                    .thenApplyAsync { responseResult: Result<String> ->
                        MlgRankResp(reqId, responseResult.getOrNull()!!)
                    }
            }
        }
        .map { respFutures ->
            val resps = respFutures.map { it.get() }
            logger.debug(
                "inserting mileage data chunk progress: [{}/{}] duration: [{}s]",
                mlgRankReqCnt.addAndGet(resps.size), totalMlgRankReqCnt, duration().toSeconds()
            )
            repo.batchInsertMlgRankResps(resps)
            resps

        }
        .flatten()

    if (requestDepth == 4) {
        repo.endJob()
        return
    }

    error("request Depth should be 1~4")
}

