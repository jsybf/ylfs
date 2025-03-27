package io.gitp.yfls.scarping.job.file

import io.gitp.ylfs.crawl.client.*
import io.gitp.ylfs.entity.type.Semester
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.runBlocking
import java.time.Year

private fun requestColleage(collegeReq: CollegeRequest): Deferred<CollegeResponse> = CollegeClient
    .request(collegeReq.toPayload())
    .thenApplyAsync { respResult: Result<String> -> CollegeResponse(collegeReq, respResult.getOrThrow()) }
    .asDeferred()

private fun requestDpt(dptReq: DptRequest): Deferred<DptResponse> {
    val dptRespAsync = DptClient
        .request(dptReq.toPayload())
        .thenApplyAsync { respResult: Result<String> -> DptResponse(dptReq, respResult.getOrThrow()) }
        .asDeferred()
    return dptRespAsync

}

private fun requestLecture(lectureReq: LectureRequest): Deferred<LectureResponse> {
    val dptRespAsync = LectureClient
        .request(lectureReq.toPayload())
        .thenApplyAsync { respResult: Result<String> -> LectureResponse(lectureReq, respResult.getOrThrow()) }
        .asDeferred()
    return dptRespAsync
}

private fun requestMlgInfo(mlgReq: MlgRequest): Deferred<MlgInfoResponse> {
    val dptRespAsync = MlgInfoClient
        .request(mlgReq.toMlgInfoPayload())
        .thenApplyAsync { respResult: Result<String> -> MlgInfoResponse(mlgReq, respResult.getOrThrow()) }
        .asDeferred()
    return dptRespAsync
}


private fun requestMlgRank(mlgReq: MlgRequest): Deferred<MlgRankResponse> {
    val dptRespAsync = MlgRankClient
        .request(mlgReq.toMlgRankPayload())
        .thenApplyAsync { respResult: Result<String> -> MlgRankResponse(mlgReq, respResult.getOrThrow()) }
        .asDeferred()
    return dptRespAsync
}

private val extractCollegeId = Regex(""" "deptCd":"(?<dptId>\w+)" """, RegexOption.COMMENTS)
private val extractDptId = Regex(""" "deptCd":"(?<dptId>\d+)" """, RegexOption.COMMENTS)
private val extractCourseId = Regex(""" "subjtnbCorsePrcts":"([\dA-Z]{7})-(\d{2})-(\d{2})" """, RegexOption.COMMENTS)

private fun parseCollegeResponse(collegeResp: CollegeResponse): List<DptRequest> {
    return extractCollegeId
        .findAll(collegeResp.resp.toString())
        .map { matchResult: MatchResult ->
            DptRequest(collegeResp.request.year, collegeResp.request.semester, matchResult.destructured.component1())
        }
        .toList()
}

private fun parseDptRsponse(dptResp: DptResponse): List<LectureRequest> =
    extractDptId
        .findAll(dptResp.resp)
        .map { matchResult -> LectureRequest(dptResp.request.year, dptResp.request.semester, dptResp.request.collegeCode, matchResult.destructured.component1()) }.toList()

private fun parseLectureResponse(lectureResp: LectureResponse): List<MlgRequest> =
    extractCourseId
        .findAll(lectureResp.resp)
        .map { matchResult: MatchResult ->
            val lecturecode = LectureCode(
                mainCode = matchResult.destructured.component1(),
                classCode = matchResult.destructured.component2(),
                subCode = matchResult.destructured.component3()
            )
            MlgRequest(lectureResp.request.year, lectureResp.request.semester, lectureResp.request.collegeCode, lectureResp.request.departmentCode, lecturecode)
        }.toList()


suspend fun job(year: Year, semester: Semester) {
    val collegeResp = requestColleage(CollegeRequest(year, semester)).await()

    val dptReqList = parseCollegeResponse(collegeResp)
    val dptRespList = dptReqList.map { requestDpt(it) }.awaitAll()

    val lectureReqList = dptRespList.map(::parseDptRsponse).flatten()
    val lectureRespList = lectureReqList.map { requestLecture(it) }.awaitAll()

    val mlgReqList = lectureRespList.map(::parseLectureResponse).flatten()
    val mlgInfoRespList = mlgReqList.map {
        delay(10)
        requestMlgInfo(it)
    }.awaitAll()
    val mlgRank = mlgReqList.map {
        delay(10)
        requestMlgRank(it)
    }.awaitAll()

}

fun main() {
    runBlocking {
        job(Year.of(2025), Semester.FIRST)
    }
}
