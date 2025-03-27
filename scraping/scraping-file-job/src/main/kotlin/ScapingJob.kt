package io.gitp.yfls.scarping.job.file

import io.gitp.ylfs.crawl.client.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import kotlinx.serialization.json.Json

fun requestColleage(collegeRequest: CollegeRequest): Deferred<CollegeResponse> {
    val responseAsync = CollegeClient
        .request(collegeRequest.toPayload())
        .thenApplyAsync { respResult: Result<String> ->
            CollegeResponse(collegeRequest, Json.decodeFromString(respResult.getOrThrow()))
        }
        .asDeferred()
    return responseAsync
}

fun requestDepartment(dptRequest: DptRequest): Deferred<DptResponse> {
    val responseAsync = DptClient
        .request(dptRequest.toPayload())
        .thenApplyAsync { respResult: Result<String> ->
            DptResponse(dptRequest, Json.decodeFromString(respResult.getOrThrow()))
        }
        .asDeferred()
    return responseAsync

}

fun requestLecture(lectureRequest: LectureRequest): Deferred<LectureResponse> {
    val responseAsync = LectureClient
        .request(lectureRequest.toPayload())
        .thenApplyAsync { respResult: Result<String> ->
            LectureResponse(lectureRequest, Json.decodeFromString(respResult.getOrThrow()))
        }
        .asDeferred()
    return responseAsync
}

fun requestMlgInfo(mileageRequest: MlgInfoRequest): Deferred<MlgInfoResponse> {
    val responseAsync = MlgInfoClient
        .request(mileageRequest.toPayload())
        .thenApplyAsync { respResult: Result<String> ->
            MlgInfoResponse(mileageRequest, Json.decodeFromString(respResult.getOrThrow()))
        }
        .asDeferred()
    return responseAsync
}


fun requestMlgRank(mlgRankRequest: MlgRankRequest): Deferred<MlgRankResponse> {
    val responseAsync = MlgRankClient
        .request(mlgRankRequest.toPayload())
        .thenApplyAsync { respResult: Result<String> ->
            MlgRankResponse(mlgRankRequest, Json.decodeFromString(respResult.getOrThrow()))
        }
        .asDeferred()
    return responseAsync
}
