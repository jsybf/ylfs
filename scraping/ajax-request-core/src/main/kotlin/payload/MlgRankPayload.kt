package io.gitp.ylfs.crawl.payload

import io.gitp.ylfs.entity.model.LectureId
import io.gitp.ylfs.entity.enums.Semester
import java.time.Year

data class MlgRankPayload(
    val lectureId: LectureId,
    val year: Year,
    val semester: Semester,
) : AbstractPayload() {

    override val defaultPayload: Map<String, String> = mapOf(
        "%40d1%23stuno" to "",
        "%40d1%23sysinstDivCd" to "H1",
        "%40d1%23appcsSchdlCd" to ""
    )

    override fun getPayloadMap(): Map<String, String> {
        val payloadMap: MutableMap<String, String> = mutableMapOf()

        payloadMap["%40d1%23syy"] = year.toString()
        payloadMap["%40d1%23smtDivCd"] = semester.code.toString()
        payloadMap["%40d1%23subjtnb"] = lectureId.mainId
        payloadMap["%40d1%23corseDvclsNo"] = lectureId.classId
        payloadMap["%40d1%23prctsCorseDvclsNo"] = lectureId.subId

        return payloadMap
    }
}