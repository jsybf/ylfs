package io.gitp.yfls.scarping.job.file.transform

import io.gitp.yfls.scarping.job.file.request.MlgRankResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

enum class Major(val raw: String) {
    MAJOR("Y(Y)"), DUAL_MAJOR_INCLUDED("Y(N)"), DUAL_MAJOR_UNINCLUDED("N(Y)"), NON_MAJOR("N(N)");

    companion object {
        fun ofRaw(raw: String): Major? = entries.find { it.raw == raw }
    }
}

@Serializable
data class MlgRankVo(
    val ifSuccess: Boolean,
    val mlgRank: Int,
    val mlgValue: Int,
    val ifDisabled: Boolean,
    val ifMajor: Major,
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
            ifSuccess = respBody["mlgAppcsPrcesDivNm"]!!.jsonPrimitive.contentOrNull?.let { MlgRankParser.parseYN2Boolean(it) } ?: true,
            mlgRank = respBody["mlgRank"]!!.jsonPrimitive.int,
            mlgValue = respBody["mlgVal"]!!.jsonPrimitive.int,
            ifDisabled = respBody["dsstdYn"]!!.jsonPrimitive.content.let { MlgRankParser.parseYN2Boolean(it) },
            ifMajor = respBody["mjsbjYn"]!!.jsonPrimitive.content.let { MlgRankParser.parseMajor(it) },
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
    fun parseMajor(raw: String): Major = Major.ofRaw(raw)!!
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