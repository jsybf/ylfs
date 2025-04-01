package io.gitp.yfls.scarping.job.file.request

import io.gitp.ylfs.crawl.client.*
import io.gitp.ylfs.entity.type.Semester
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.asDeferred
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Year
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

private object RequestFuncs {
    fun requestColleage(collegeReq: CollegeRequest): Deferred<CollegeResponse> = CollegeClient
        .request(collegeReq.toPayload())
        .thenApplyAsync { respResult: Result<String> -> CollegeResponse(collegeReq, Json.decodeFromString(respResult.getOrThrow())) }
        .asDeferred()

    fun requestDpt(dptReq: DptRequest): Deferred<DptResponse> {
        val dptRespAsync = DptClient
            .request(dptReq.toPayload())
            .thenApplyAsync { respResult: Result<String> -> DptResponse(dptReq, Json.decodeFromString(respResult.getOrThrow())) }
            .asDeferred()
        return dptRespAsync

    }

    fun requestLecture(lectureReq: LectureRequest): Deferred<LectureResponse> {
        val dptRespAsync = LectureClient
            .request(lectureReq.toPayload())
            .thenApplyAsync { respResult: Result<String> -> LectureResponse(lectureReq, Json.decodeFromString(respResult.getOrThrow())) }
            .asDeferred()
        return dptRespAsync
    }

    fun requestMlgInfo(mlgReq: MlgRequest): Deferred<MlgInfoResponse> {
        val dptRespAsync = MlgInfoClient
            .request(mlgReq.toMlgInfoPayload())
            .thenApplyAsync { respResult: Result<String> -> MlgInfoResponse(mlgReq, Json.decodeFromString(respResult.getOrThrow())) }
            .asDeferred()
        return dptRespAsync
    }


    fun requestMlgRank(mlgReq: MlgRequest): Deferred<MlgRankResponse> {
        val dptRespAsync = MlgRankClient
            .request(mlgReq.toMlgRankPayload())
            .thenApplyAsync { respResult: Result<String> -> MlgRankResponse(mlgReq, Json.decodeFromString(respResult.getOrThrow())) }
            .asDeferred()
        return dptRespAsync
    }
}

private object ResponseParsers {
    fun parseCollegeResponse(collegeResp: CollegeResponse): List<DptRequest> =
        collegeResp.resp
            .jsonObject["dsUnivCd"]!!
            .jsonArray
            .map { jsonObj ->
                val collegeCode = jsonObj.jsonObject["deptCd"]!!.jsonPrimitive.content
                DptRequest(collegeResp.request.year, collegeResp.request.semester, collegeCode)
            }

    fun parseDptRsponse(dptResp: DptResponse): List<LectureRequest> =
        dptResp.resp
            .jsonObject["dsFaclyCd"]!!
            .jsonArray
            .map { jsonObj ->
                val dptCode = jsonObj.jsonObject["deptCd"]!!.jsonPrimitive.content
                LectureRequest(dptResp.request.year, dptResp.request.semester, dptResp.request.collegeCode, dptCode)
            }

    fun parseLectureResponse(lectureResp: LectureResponse): List<MlgRequest> =
        lectureResp.resp
            .jsonObject["dsSles251"]!!
            .jsonArray
            .map { jsonObj ->
                val lecturecode = LectureCode(
                    mainCode = jsonObj.jsonObject["subjtnb"]!!.jsonPrimitive.content,
                    classCode = jsonObj.jsonObject["corseDvclsNo"]!!.jsonPrimitive.content,
                    subCode = jsonObj.jsonObject["prctsCorseDvclsNo"]!!.jsonPrimitive.content,
                )
                MlgRequest(lectureResp.request.year, lectureResp.request.semester, lectureResp.request.collegeCode, lectureResp.request.dptCode, lecturecode)
            }
}


private fun saveToFile(filePath: Path, content: String) {
    filePath.writeText(content)
}

private val logger = LoggerFactory.getLogger(object {}::class.java)
suspend fun job(year: Year, semester: Semester, basePath: Path, delayMs: Long = 4) {

    basePath.createDirectories()

    /* college */
    logger.info("scraping college")
    val collegeResp: CollegeResponse = RequestFuncs.requestColleage(CollegeRequest(year, semester)).await()
    saveToFile(basePath.resolve("college.json"), Json.encodeToString(collegeResp))


    /* deparment */
    logger.info("scraping department")
    val dptReqList = ResponseParsers.parseCollegeResponse(collegeResp)
    val dptRespList = dptReqList.map { RequestFuncs.requestDpt(it) }.awaitAll()
    saveToFile(basePath.resolve("dpt.json"), Json.encodeToString(dptRespList))

    /* lecture */
    logger.info("scraping lecture")
    val lectureReqList = dptRespList.map { ResponseParsers.parseDptRsponse(it) }.flatten()
    val lectureRespList = lectureReqList.map { RequestFuncs.requestLecture(it) }.awaitAll()
    saveToFile(basePath.resolve("lecture.json"), Json.encodeToString(lectureRespList))

    /* mileage info */
    logger.info("scraping mileage info")
    val mlgReqList = lectureRespList.map { ResponseParsers.parseLectureResponse(it) }.flatten()
    val mlgInfoRespList = mlgReqList.map {
        delay(delayMs)
        RequestFuncs.requestMlgInfo(it)
    }.awaitAll()
    saveToFile(basePath.resolve("mlg-info.json"), Json.encodeToString(mlgInfoRespList))

    /* milage rank */
    logger.info("scraping mileage rank")
    val mlgRankRespList = mlgReqList.map {
        delay(delayMs)
        RequestFuncs.requestMlgRank(it)
    }.awaitAll()
    saveToFile(basePath.resolve("mlg-rank.json"), Json.encodeToString(mlgRankRespList))

}
