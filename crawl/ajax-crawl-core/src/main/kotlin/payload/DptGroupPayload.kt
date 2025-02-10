package io.gitp.ylfs.crawl.payload

import io.gitp.ylfs.entity.type.Semester
import java.time.Year

data class DptGroupPayload(
    val year: Year,
    val semester: Semester
) : AbstractPayload() {

    override val defaultPayload: Map<String, String> = mapOf(
        "%40d1%23dsNm" to "dsUnivCd",
        "%40d1%23level" to "B",
        "%40d1%23lv1" to "s1",
        "%40d1%23lv2" to "%25",
        "%40d1%23lv3" to "%25",
        "%40d1%23univGbn" to "A",
        "%40d1%23findAuthGbn" to "8",
        "%40d1%23sysinstDivCd" to "%25"
    )

    override fun getPayloadMap(): Map<String, String> {
        val payloadMap: MutableMap<String, String> = mutableMapOf()
        payloadMap["%40d1%23syy"] = year.toString()
        payloadMap["%40d1%23smtDivCd"] = semester.code.toString()

        return payloadMap
    }

}