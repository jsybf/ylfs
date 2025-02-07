package io.gitp.ysfl.client.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Dpt(
    @SerialName("deptNm")
    val dptName: String,
    @SerialName("deptCd")
    val dptId: String
) : YonseiResp
