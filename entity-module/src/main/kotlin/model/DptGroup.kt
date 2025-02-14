package io.gitp.ylfs.entity.model

import io.gitp.ylfs.entity.type.LectureId
import io.gitp.ylfs.entity.type.Semester
import java.time.DayOfWeek
import java.time.Year

data class DptGroup(
    val dptGroupId: String,
    val name: String,
    val year: Year,
    val semester: Semester,
) {
    init {
        require(name.length == 6)
    }
}

data class Dpt(
    val dptId: String,
    val name: String,
)

data class Course(
    val name: String,
    val lectureId: LectureId,

    val professors: List<String>,
    val schedules: Map<Period, LocationUnion>,

    val courseType: CourseType
)

/**
 * 공기(공통기초), 교기(교양기초)
 * 필교(필수교양), 대학교양(대교)
 * 전기, 전선, 전필
 */
enum class CourseType(
)

data class Period(
    val day: DayOfWeek,
    val period: Int
)


sealed interface LocationUnion {
    data object RealTimeOnline : LocationUnion
    data class Online(val duplicateCapability: Boolean) : LocationUnion
    data class OffLine(val building: String, val address: String?) : LocationUnion
}


