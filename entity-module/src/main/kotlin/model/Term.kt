package io.gitp.ylfs.entity.model

import io.gitp.ylfs.entity.enums.Semester
import java.time.Year

data class Term(
    val year: Year,
    val semester: Semester,
) {
    var dbId: Int? = null
}
