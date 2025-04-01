package io.gitp.yfls.scarping.job.file.transform

import io.gitp.yfls.scarping.job.file.YearSerializer
import io.gitp.yfls.scarping.job.file.request.MlgInfoResponse
import io.gitp.ylfs.entity.enums.Semester
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Year

enum class MajorProtectType { ONLY_MAJOR, ALSO_DUAL_MAJOR, UNPROTECT }

@Serializable
data class MlgInfo(
    @Serializable(with = YearSerializer::class)
    val year: Year,
    val semester: Semester,

    val mainCode: String,
    val classCode: String,
    val subCode: String,

    val mileageLimit: Int,
    val majorProtectType: MajorProtectType,

    val appliedCnt: Int,
    val totalCapacity: Int,
    val majorCapacity: Int,
    val perMajorCapacity: List<Int>
) {
    init {
        require(perMajorCapacity.size == 6)
    }
}

fun transformMlgInfoResp(mlgInfoResp: MlgInfoResponse): MlgInfo? {
    val respBody = mlgInfoResp.resp["dsSles251"]!!.jsonArray.firstOrNull()?.jsonObject ?: return null

    return MlgInfo(
        year = mlgInfoResp.request.year,
        semester = mlgInfoResp.request.semester,

        mainCode = mlgInfoResp.request.lectureCode.mainCode,
        classCode = mlgInfoResp.request.lectureCode.classCode,
        subCode = mlgInfoResp.request.lectureCode.subCode,

        mileageLimit = respBody["usePosblMaxMlgVal"]!!.jsonPrimitive.int,
        majorProtectType = respBody["mjrprPercpCnt"]!!.jsonPrimitive.content.let { MlgInfoParser.parseMajorProtectType(it) },

        appliedCnt = respBody["cnt"]!!.jsonPrimitive.int,
        totalCapacity = respBody["atnlcPercpCnt"]!!.jsonPrimitive.int,
        majorCapacity = respBody["mjrprPercpCnt"]!!.jsonPrimitive.content.let { MlgInfoParser.parseMajorCap(it) },
        perMajorCapacity = listOf(
            respBody["sy1PercpCnt"]!!.jsonPrimitive.int,
            respBody["sy2PercpCnt"]!!.jsonPrimitive.int,
            respBody["sy3PercpCnt"]!!.jsonPrimitive.int,
            respBody["sy4PercpCnt"]!!.jsonPrimitive.int,
            respBody["sy5PercpCnt"]!!.jsonPrimitive.int,
            respBody["sy6PercpCnt"]!!.jsonPrimitive.int,
        )
    )
}

object MlgInfoParser {
    private val capture: Regex = Regex("""(\d)\((Y|N)\)""")

    fun parseMajorCap(raw: String): Int = capture.find(raw)!!.groupValues[1].toInt()

    fun parseMajorProtectType(raw: String): MajorProtectType {
        val (capacity: Int, ifDualMajorYn: String) = capture.find(raw)!!.destructured.let { (capStr: String, ifDualMajorEnabled: String) -> Pair(capStr.toInt(), ifDualMajorEnabled) }
        return when {
            capacity == 0 -> MajorProtectType.UNPROTECT
            0 < capacity && ifDualMajorYn == "N" -> MajorProtectType.ONLY_MAJOR
            0 < capacity && ifDualMajorYn == "Y" -> MajorProtectType.ALSO_DUAL_MAJOR
            else -> throw IllegalStateException("parsing MajorProtectType fucked up. got:$raw")
        }
    }
}