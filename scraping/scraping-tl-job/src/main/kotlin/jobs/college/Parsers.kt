package io.gitp.ylfs.scraping.scraping_tl_job.jobs.college

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object CollegeRespParser {
    fun toCollegeDtos(collegeRespDto: CollegeRespDto): List<CollegeDto> {
        val collegeJsonArray = collegeRespDto.resp.jsonObject["dsUnivCd"]!!.jsonArray

        return collegeJsonArray.map { collegeJson ->
            val collegeName = collegeJson.jsonObject["deptNm"]!!.jsonPrimitive.content
            val collegeCode = collegeJson.jsonObject["deptCd"]!!.jsonPrimitive.content

            CollegeDto(
                year = collegeRespDto.year,
                semester = collegeRespDto.semester,
                name = collegeName,
                code = collegeCode
            )
        }
    }
}
