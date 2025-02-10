package io.gitp.ylfs.crawl.payload

import io.gitp.ylfs.entity.type.Semester
import java.time.Year

data class CoursePayload(
    val dptGroupId: String,
    val dptId: String,
    val year: Year,
    val semester: Semester
) : AbstractPayload() {

    override val defaultPayload: Map<String, String> = mapOf(
        "%40d1%23campsBusnsCd" to "s1",
        "%40d1%23hy" to "",
        "%40d1%23cdt" to "%25",
        "%40d1%23kwdDivCd" to "1",
        "%40d1%23searchGbn" to "1",
        "%40d1%23kwd" to "",
        "%40d1%23allKwd" to "",
        "%40d1%23engChg" to "",
        "%40d1%23prnGbn" to "false",
        "%40d1%23lang" to "",
        "%40d1%23campsDivCd" to "S",
        "%40d1%23stuno" to ""
    )

    override fun getPayloadMap(): Map<String, String> {
        val payloadMap: MutableMap<String, String> = mutableMapOf()

        payloadMap["%40d1%23syy"] = year.toString()
        payloadMap["%40d1%23smtDivCd"] = semester.code.toString()
        payloadMap["%40d1%23univCd"] = dptGroupId
        payloadMap["%40d1%23faclyCd"] = dptId

        return payloadMap
    }
}