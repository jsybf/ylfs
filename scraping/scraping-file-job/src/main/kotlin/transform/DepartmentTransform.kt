package io.gitp.yfls.scarping.job.file.transform

import io.gitp.yfls.scarping.job.file.YearSerializer
import io.gitp.yfls.scarping.job.file.request.DptResponse
import io.gitp.ylfs.entity.type.Semester
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.time.Year
import kotlin.io.path.readText


@Serializable
data class DptVo(
    @Serializable(with = YearSerializer::class)
    val year: Year,
    val semester: Semester,
    val collegeCode: String,

    val name: String,
    val dptCode: String
)

fun transformDpt(dpt: DptResponse): List<DptVo> =
    dpt
        .resp["dsFaclyCd"]!!
        .jsonArray
        .map { dptResp: JsonElement ->
            val dptName: String = dptResp.jsonObject["deptNm"]!!.jsonPrimitive.content
            val dptCode: String = dptResp.jsonObject["deptCd"]!!.jsonPrimitive.content
            DptVo(year = dpt.request.year, semester = dpt.request.semester, collegeCode = dpt.request.collegeCode, name = dptName, dptCode = dptCode)
        }