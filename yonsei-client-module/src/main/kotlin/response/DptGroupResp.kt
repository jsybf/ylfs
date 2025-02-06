package io.gitp.ysfl.client.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DptGroupResp(
    @SerialName("deptNm")
    val dptGroupName: String,
    @SerialName("deptCd")
    val dptGroupId: String
)
