package io.gitp.yfls.scarping.job.file

import io.gitp.ylfs.crawl.payload.*
import io.gitp.ylfs.entity.type.LectureId
import io.gitp.ylfs.entity.type.Semester
import kotlinx.serialization.json.JsonObject
import java.time.Year

data class CollegeRequest(
    val year: Year,
    val semester: Semester,
) {
    fun toPayload(): CollegePayload = CollegePayload(year, semester)
}

data class DptRequest(
    val year: Year,
    val semester: Semester,
    val collegeCode: String
) {
    fun toPayload(): DptPayload = DptPayload(collegeCode, year, semester)
}

data class LectureRequest(
    val year: Year,
    val semester: Semester,
    val collegeCode: String,
    val departmentCode: String
) {
    fun toPayload(): LecturePayload = LecturePayload(collegeCode, departmentCode, year, semester)
}

data class LectureCode(
    val mainCode: String,
    val classCoce: String,
    val subCode: String,
) {
    fun toLectureId(): LectureId = LectureId(mainCode, classCoce, subCode)
}

data class MlgInfoRequest(
    val year: Year,
    val semester: Semester,
    val collegeCode: String,
    val departmentCode: String,
    val lectureCode: LectureCode
) {
    fun toPayload(): MlgInfoPayload = MlgInfoPayload(lectureCode.toLectureId(), year, semester)
}


data class MlgRankRequest(
    val year: Year,
    val semester: Semester,
    val collegeCode: String,
    val departmentCode: String,
    val lectureCode: LectureCode
) {
    fun toPayload(): MlgRankPayload = MlgRankPayload(lectureCode.toLectureId(), year, semester)
}

data class CollegeResponse(
    val request: CollegeRequest,
    val resp: JsonObject
)

data class DptResponse(
    val request: DptRequest,
    val resp: JsonObject
)

data class LectureResponse(
    val request: LectureRequest,
    val resp: JsonObject
)

data class MlgInfoResponse(
    val request: MlgInfoRequest,
    val resp: JsonObject
)

data class MlgRankResponse(
    val request: MlgRankRequest,
    val resp: JsonObject
)

