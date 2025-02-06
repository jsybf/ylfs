package io.gitp.ysfl.db.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DptResp(
    @SerialName("deptNm")
    val dptName: String,
    @SerialName("deptCd")
    val dptId: String
)
