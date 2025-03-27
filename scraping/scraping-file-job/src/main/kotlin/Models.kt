package io.gitp.yfls.scarping.job.file

import io.gitp.ylfs.crawl.payload.*
import io.gitp.ylfs.entity.type.LectureId
import io.gitp.ylfs.entity.type.Semester
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
    val classCode: String,
    val subCode: String,
) {
    fun toLectureId(): LectureId = LectureId(mainCode, classCode, subCode)
}

data class MlgRequest(
    val year: Year,
    val semester: Semester,
    val collegeCode: String,
    val departmentCode: String,
    val lectureCode: LectureCode
) {
    fun toMlgInfoPayload(): MlgInfoPayload = MlgInfoPayload(lectureCode.toLectureId(), year, semester)
    fun toMlgRankPayload(): MlgRankPayload = MlgRankPayload(lectureCode.toLectureId(), year, semester)
}

data class CollegeResponse(
    val request: CollegeRequest,
    val resp: String
)

data class DptResponse(
    val request: DptRequest,
    val resp: String
)

data class LectureResponse(
    val request: LectureRequest,
    val resp: String
)

data class MlgInfoResponse(
    val request: MlgRequest,
    val resp: String
)

data class MlgRankResponse(
    val request: MlgRequest,
    val resp: String
)

