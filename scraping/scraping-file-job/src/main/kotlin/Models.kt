package io.gitp.yfls.scarping.job.file

import io.gitp.ylfs.crawl.payload.*
import io.gitp.ylfs.entity.type.LectureId
import io.gitp.ylfs.entity.type.Semester
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.Year

@Serializable
data class CollegeRequest(
    @Serializable(with = YearSerializer::class)
    val year: Year,
    val semester: Semester,
) {
    fun toPayload(): CollegePayload = CollegePayload(year, semester)
}

@Serializable
data class DptRequest(
    @Serializable(with = YearSerializer::class)
    val year: Year,
    val semester: Semester,
    val collegeCode: String
) {
    fun toPayload(): DptPayload = DptPayload(collegeCode, year, semester)
}

@Serializable
data class LectureRequest(
    @Serializable(with = YearSerializer::class)
    val year: Year,
    val semester: Semester,
    val collegeCode: String,
    val dptCode: String
) {
    fun toPayload(): LecturePayload = LecturePayload(collegeCode, dptCode, year, semester)
}

@Serializable
data class LectureCode(
    val mainCode: String,
    val classCode: String,
    val subCode: String,
) {
    fun toLectureId(): LectureId = LectureId(mainCode, classCode, subCode)
}

@Serializable
data class MlgRequest(
    @Serializable(with = YearSerializer::class)
    val year: Year,
    val semester: Semester,
    val collegeCode: String,
    val dptCode: String,
    val lectureCode: LectureCode
) {
    fun toMlgInfoPayload(): MlgInfoPayload = MlgInfoPayload(lectureCode.toLectureId(), year, semester)
    fun toMlgRankPayload(): MlgRankPayload = MlgRankPayload(lectureCode.toLectureId(), year, semester)
}

@Serializable
data class CollegeResponse(
    val request: CollegeRequest,
    val resp: JsonObject
)

@Serializable
data class DptResponse(
    val request: DptRequest,
    val resp: JsonObject
)

@Serializable
data class LectureResponse(
    val request: LectureRequest,
    val resp: JsonObject
)

@Serializable
data class MlgInfoResponse(
    val request: MlgRequest,
    val resp: JsonObject
)

@Serializable
data class MlgRankResponse(
    val request: MlgRequest,
    val resp: JsonObject
)

