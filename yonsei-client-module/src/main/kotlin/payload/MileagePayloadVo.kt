package io.gitp.ysfl.client.payload

import io.gitp.ysfl.client.Semester
import io.gitp.ysfl.client.response.LectureId
import java.time.Year

public class MileagePayloadVo(
    val year: Year,
    val semester: Semester,
    val lectureId: LectureId
) : AbstractPayloadVo() {

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
        payloadMap["%40d1%23corseDvclsNo"] = lectureId.classDivisionId
        payloadMap["%40d1%23prctsCorseDvclsNo"] = lectureId.subId

        return payloadMap
    }
}