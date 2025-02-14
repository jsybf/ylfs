package io.gitp.ylfs.crawl.crawljob

import io.gitp.ylfs.entity.type.LectureId

internal data class DptReqId(val collegeId: String)

internal data class LectureReqId(val collegeId: String, val dptId: String)

internal data class MlgRankReqId(val lectureId: LectureId)


internal data class CollegeResp(
    val resp: String
)

internal data class DptResp(
    val reqId: DptReqId,
    val resp: String
)

internal data class LectureResp(
    val reqId: LectureReqId,
    val resp: String
)

internal data class MlgRankResp(
    val reqId: MlgRankReqId,
    val resp: String
)
