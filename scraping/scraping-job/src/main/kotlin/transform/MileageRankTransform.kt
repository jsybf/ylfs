package io.gitp.yfls.scarping.job.file.transform

import io.gitp.yfls.scarping.job.file.YearSerializer
import io.gitp.yfls.scarping.job.file.request.MlgRankResponse
import io.gitp.ylfs.entity.enums.Semester
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.time.Year
import kotlin.io.path.readText
import kotlin.io.path.writeText

enum class MajorProtectedType(val raw: String) {
    MAJOR_PROTECTED("Y(Y)"), DUAL_MAJOR_PROTECTED("Y(N)"), DUAL_MAJOR_NOT_PROTECTED("N(Y)"), NOT_PROTECTED("N(N)");

    companion object {
        fun ofRaw(raw: String): MajorProtectedType? = entries.find { it.raw == raw }
    }
}

@Serializable
data class MlgRankVo(
    @Serializable(with = YearSerializer::class)
    val year: Year,
    val semester: Semester,
    val mainCode: String,
    val classCode: String,
    val subCode: String,

    val ifSuccess: Boolean,
    val mlgRank: Int,
    val mlgValue: Int,
    val ifDisabled: Boolean,
    val ifMajorProtected: MajorProtectedType,
    val appliedSubjectCnt: Int,
    val ifGradePlanned: Boolean,
    val ifFirstApply: Boolean,
    val lastSemesterRatio: Float,
    val lastSemesterratioFrac: String,
    val totalCreditRatio: Float,
    val totalCreditRatioFrac: String
)

fun transformMlgRankResp(mlgRankResp: MlgRankResponse): List<MlgRankVo> {
    val respBodyList = mlgRankResp.resp["dsSles440"]!!.jsonArray.map { it.jsonObject }

    return respBodyList.map { respBody ->
        MlgRankVo(
            year = mlgRankResp.request.year,
            semester = mlgRankResp.request.semester,

            mainCode = mlgRankResp.request.lectureCode.mainCode,
            classCode = mlgRankResp.request.lectureCode.classCode,
            subCode = mlgRankResp.request.lectureCode.subCode,

            ifSuccess = respBody["mlgAppcsPrcesDivNm"]!!.jsonPrimitive.contentOrNull?.let { MlgRankParser.parseYN2Boolean(it) } ?: true,
            mlgRank = respBody["mlgRank"]!!.jsonPrimitive.int,
            mlgValue = respBody["mlgVal"]!!.jsonPrimitive.int,
            ifDisabled = respBody["dsstdYn"]!!.jsonPrimitive.content.let { MlgRankParser.parseYN2Boolean(it) },
            ifMajorProtected = respBody["mjsbjYn"]!!.jsonPrimitive.content.let { MlgRankParser.parseMajor(it) },
            appliedSubjectCnt = respBody["aplySubjcCnt"]!!.jsonPrimitive.int,
            ifGradePlanned = respBody["grdtnAplyYn"]!!.jsonPrimitive.content.let { MlgRankParser.parseYN2Boolean(it) },
            ifFirstApply = respBody["fratlcYn"]!!.jsonPrimitive.content.let { MlgRankParser.parseYN2Boolean(it) },
            lastSemesterRatio = respBody["jstbfSmtCmpsjCdtRto"]!!.jsonPrimitive.float,
            lastSemesterratioFrac = respBody["jstbfSmtCmpsjAtnlcPosblCdt"]!!.jsonPrimitive.content,
            totalCreditRatio = respBody["ttCmpsjCdtRto"]!!.jsonPrimitive.float,
            totalCreditRatioFrac = respBody["ttCmpsjGrdtnCmpsjCdt"]!!.jsonPrimitive.content,
        )
    }
}

object MlgRankParser {
    fun parseMajor(raw: String): MajorProtectedType = MajorProtectedType.ofRaw(raw)!!
    fun parseYN2Boolean(raw: String): Boolean = when (raw) {
        "Y" -> true
        "N" -> false
        else -> throw IllegalStateException("allowed: 'Y', 'N' got:$raw")
    }
}

fun main() {
    val mlgRankRespJsonText: String = Path.of("data-2/23-2/mlg-rank.json").toAbsolutePath().normalize().readText()
    val mlgRespList: List<MlgRankResponse> = Json.decodeFromString<List<MlgRankResponse>>(mlgRankRespJsonText)
    val mlgRankList = mlgRespList.flatMap { transformMlgRankResp(it) }.onEach { println(it) }

    val mlgRankListStr: String = Json.encodeToString<List<MlgRankVo>>(mlgRankList)
    Path.of("data-2/23-2/mlg-rank-refined.json").toAbsolutePath().normalize().writeText(mlgRankListStr)
}