package io.gitp.ysfl.client.payload

import io.gitp.ysfl.client.Semester
import java.time.Year

class DptPayloadVo(
    val departmentGroupId: String,
    val year: Year,
    val semester: Semester
) : AbstractPayloadVo() {
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
        payLoadMap["%40d1%23lv2"] = departmentGroupId
        payLoadMap["%40d1%23syy"] = year.toString()
        payLoadMap["%40d1%23smtDivCd"] = semester.code.toString()

        return payLoadMap
    }

}
