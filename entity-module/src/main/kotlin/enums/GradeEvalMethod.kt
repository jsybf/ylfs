package io.gitp.ylfs.entity.enums

import kotlinx.serialization.Serializable

@Serializable
enum class GradeEvalMethod {
    P_OR_NP, ABSOLUTE, RELATIVE, NONE
}