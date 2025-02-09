package io.gitp.ysfl.client.response

import io.gitp.ysfl.client.deserializer.LectureDeserializer
import kotlinx.serialization.Serializable
import java.time.DayOfWeek

@Serializable(with = LectureDeserializer::class)
data class Lecture(
    val lectureId: LectureId,
    val dptId: String,
    val name: String,

    val locationAndSchedule: Map<Schedule, LocationUnion>,

    val professors: List<String>,
)

data class LectureId(
    val mainId: String,
    val classDivisionId: String,
    val subId: String
)

data class Schedule(
    val dayOfWeek: DayOfWeek,
    val periods: List<Int>
)

sealed interface LocationUnion {
    data class RealTimeOnline(val dumpy: Nothing? = null) : LocationUnion
    data class Online(val duplicateCapability: Boolean) : LocationUnion
    data class OffLine(val building: String, val address: String?) : LocationUnion
}
