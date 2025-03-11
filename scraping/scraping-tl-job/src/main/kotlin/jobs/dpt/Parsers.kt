package io.gitp.ylfs.scraping.scraping_tl_job.jobs.dpt

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object DptRespParser {
    fun parse(dptResp: DptRespDto): List<DptDto> {
        val dptJsonArray = dptResp.resp.jsonObject["dsFaclyCd"]!!.jsonArray

        return dptJsonArray.map { dptJson ->
            DptDto(
                year = dptResp.year,
                semester = dptResp.semester,
                collegeCode = dptResp.collegeCode,
                code = dptJson.jsonObject["deptCd"]!!.jsonPrimitive.content,
                name = dptJson.jsonObject["deptNm"]!!.jsonPrimitive.content,
            )
        }
    }
}
