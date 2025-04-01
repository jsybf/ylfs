package io.gitp.yfls.scarping.job.file.transform

import io.gitp.yfls.scarping.job.file.YearSerializer
import io.gitp.yfls.scarping.job.file.request.CollegeResponse
import io.gitp.ylfs.entity.enums.Semester
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Year


@Serializable
data class CollegeVo(
    @Serializable(with = YearSerializer::class)
    val year: Year,
    val semester: Semester,

    val name: String,
    val collegeCode: String,
)

fun transformCollege(college: CollegeResponse): List<CollegeVo> =
    college
        .resp["dsUnivCd"]!!
        .jsonArray
        .map { dptResp: JsonElement ->
            val collegeName: String = dptResp.jsonObject["deptNm"]!!.jsonPrimitive.content
            val collegeCode: String = dptResp.jsonObject["deptCd"]!!.jsonPrimitive.content
            CollegeVo(year = college.request.year, semester = college.request.semester, name = collegeName, collegeCode = collegeCode)
        }
