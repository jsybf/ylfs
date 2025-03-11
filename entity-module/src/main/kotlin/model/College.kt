package io.gitp.ylfs.entity.model

data class College(
    val code: String,
    val name: String,
    val term: Term,
) {
    var dbId: Int? = null
}