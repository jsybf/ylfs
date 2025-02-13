package io.gitp.ylfs.crawl.payload

import io.gitp.ylfs.entity.type.CourseId
import io.gitp.ylfs.entity.type.Semester
import java.time.Year

data class MileagePayload(
    val courseId: CourseId,
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
        payloadMap["%40d1%23subjtnb"] = courseId.mainId
        payloadMap["%40d1%23corseDvclsNo"] = courseId.classId
        payloadMap["%40d1%23prctsCorseDvclsNo"] = courseId.subId

        return payloadMap
    }
}