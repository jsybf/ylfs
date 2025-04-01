package io.gitp.ylfs.crawl.payload

import io.gitp.ylfs.entity.enums.Semester
import java.time.Year

data class DptPayload(
    val collegeId: String,
    val year: Year,
    val semester: Semester
) : AbstractPayload() {

    init {
        // 신촌캠(본캠) 학과들은 dptGroupId가 s로 시작함
        require(collegeId.startsWith("s"))
    }

    override val defaultPayload: Map<String, String> = mapOf(
        "%40d1%23dsNm" to "dsFaclyCd",
        "%40d1%23level" to "B",
        "%40d1%23lv1" to "s1",
        "%40d1%23lv3" to "%25",
        "%40d1%23univGbn" to "A",
        "%40d1%23findAuthGbn" to "8",
        "%40d1%23sysinstDivCd" to "%25",
    )

    override fun getPayloadMap(): Map<String, String> {
        val payLoadMap: MutableMap<String, String> = mutableMapOf()

        payLoadMap["%40d1%23lv2"] = collegeId
        payLoadMap["%40d1%23syy"] = year.toString()
        payLoadMap["%40d1%23smtDivCd"] = semester.code.toString()

        return payLoadMap
    }

}
