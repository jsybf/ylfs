package io.gitp.yfls.scarping.job.file

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
import java.nio.file.Path
import java.time.Year
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

private fun requestColleage(collegeReq: CollegeRequest): Deferred<CollegeResponse> = CollegeClient
    .request(collegeReq.toPayload())
    .thenApplyAsync { respResult: Result<String> -> CollegeResponse(collegeReq, Json.decodeFromString(respResult.getOrThrow())) }
    .asDeferred()

private fun requestDpt(dptReq: DptRequest): Deferred<DptResponse> {
    val dptRespAsync = DptClient
        .request(dptReq.toPayload())
        .thenApplyAsync { respResult: Result<String> -> DptResponse(dptReq, Json.decodeFromString(respResult.getOrThrow())) }
        .asDeferred()
    return dptRespAsync

}

private fun requestLecture(lectureReq: LectureRequest): Deferred<LectureResponse> {
    val dptRespAsync = LectureClient
        .request(lectureReq.toPayload())
        .thenApplyAsync { respResult: Result<String> -> LectureResponse(lectureReq, Json.decodeFromString(respResult.getOrThrow())) }
        .asDeferred()
    return dptRespAsync
}

private fun requestMlgInfo(mlgReq: MlgRequest): Deferred<MlgInfoResponse> {
    val dptRespAsync = MlgInfoClient
        .request(mlgReq.toMlgInfoPayload())
        .thenApplyAsync { respResult: Result<String> -> MlgInfoResponse(mlgReq, Json.decodeFromString(respResult.getOrThrow())) }
        .asDeferred()
    return dptRespAsync
}


private fun requestMlgRank(mlgReq: MlgRequest): Deferred<MlgRankResponse> {
    val dptRespAsync = MlgRankClient
        .request(mlgReq.toMlgRankPayload())
        .thenApplyAsync { respResult: Result<String> -> MlgRankResponse(mlgReq, Json.decodeFromString(respResult.getOrThrow())) }
        .asDeferred()
    return dptRespAsync
}

private fun parseCollegeResponse(collegeResp: CollegeResponse): List<DptRequest> =
    collegeResp.resp
        .jsonObject["dsUnivCd"]!!
        .jsonArray
        .map { jsonObj ->
            val collegeCode = jsonObj.jsonObject["deptCd"]!!.jsonPrimitive.content
            DptRequest(collegeResp.request.year, collegeResp.request.semester, collegeCode)
        }

private fun parseDptRsponse(dptResp: DptResponse): List<LectureRequest> =
    dptResp.resp
        .jsonObject["dsFaclyCd"]!!
        .jsonArray
        .map { jsonObj ->
            val dptCode = jsonObj.jsonObject["deptCd"]!!.jsonPrimitive.content
            LectureRequest(dptResp.request.year, dptResp.request.semester, dptResp.request.collegeCode, dptCode)
        }

private fun parseLectureResponse(lectureResp: LectureResponse): List<MlgRequest> =
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

private fun saveToFile(filePath: Path, content: String) {
    filePath.writeText(content)
}

private fun buildJsonArr(jsonObjStrList: List<String>): String = buildString {
    append("[")
    for (i in jsonObjStrList.indices) {
        append(jsonObjStrList[i])
        if (i != jsonObjStrList.size - 1) append(",")

    }
    append("]")
}

suspend fun job(year: Year, semester: Semester, basePath: Path, delay: Long = 4) {
    basePath.createDirectories()

    val collegeResp: CollegeResponse = requestColleage(CollegeRequest(year, semester)).await()
    saveToFile(basePath.resolve("college.json"), Json.encodeToString(collegeResp))


    val dptReqList = parseCollegeResponse(collegeResp)
    val dptRespList = dptReqList.map { requestDpt(it) }.awaitAll()
    saveToFile(basePath.resolve("dpt.json"), Json.encodeToString(dptRespList))

    val lectureReqList = dptRespList.map(::parseDptRsponse).flatten()
    val lectureRespList = lectureReqList.map { requestLecture(it) }.awaitAll()
    saveToFile(basePath.resolve("lecture.json"), Json.encodeToString(lectureRespList))

    val mlgReqList = lectureRespList.map(::parseLectureResponse).flatten()
    val mlgInfoRespList = mlgReqList.map {
        delay(delay)
        requestMlgInfo(it)
    }.awaitAll()
    saveToFile(basePath.resolve("mlg-info.json"), Json.encodeToString(mlgInfoRespList))

    val mlgRankRespList = mlgReqList.map {
        delay(delay)
        requestMlgRank(it)
    }.awaitAll()
    saveToFile(basePath.resolve("mlg-rank.json"), Json.encodeToString(mlgRankRespList))

}
