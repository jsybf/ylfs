package io.gitp.ylfs.entity.model

data class Dpt(
    val college: College,
    val code: String,
    val name: String,
) {
    var dbId: Int? = null
}