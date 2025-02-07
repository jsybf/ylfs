package io.gitp.ysfl.client.response

import io.gitp.ysfl.client.deserializer.LectureDeserializer
import kotlinx.serialization.Serializable

@Serializable(with = LectureDeserializer::class)
data class Lecture(
    val lectureId: LectureId,
    val dptId: String,
    val name: String,

    val classrooms: List<ClassroomUnion>,
    val schedules: Map<Char, List<Int>>,

    val professors: List<String>,
)

data class LectureId(
    val mainId: String,
    val classDivisionId: String,
    val subId: String
)

sealed interface ClassroomUnion {
    data class RealTimeOnline(val dumpy: Nothing? = null) : ClassroomUnion
    data class Online(val duplicateCapability: Boolean) : ClassroomUnion

    data class OffLine(
        val building: String,
        val address: String?
    ) : ClassroomUnion {

        companion object {
            // order matters
            private val regexList = listOf(
                Regex("""(제[0-9]강의실)"""), // edge case 1
                Regex("""(KLI[0-9]F-?[0-9]?)"""), // edge case 2
                Regex("""^([A-Za-z\-0-9]+)$"""), // edge case 3 ex) IBS610
                Regex("""^([가-힣]+)$"""), // normal case 1  ex) 백양누리광장, 석산홀세미나실
                Regex("""^([가-힣]+[a-zA-Z]{0,2})([0-9]+-?[A-Z0-9]*)$"""), // normal case 2 ex) 대별B101, 삼312,외627-1
                Regex("""^([가-힣]+[0-9]{0,2})-([가-힣]+[0-9]{0,3})$"""), // normal case 2 ex) 대별B101, 삼312,외627-1
            )

            internal fun of(raw: String): OffLine = regexList
                .firstNotNullOfOrNull { regex: Regex -> regex.find(raw) }
                ?.groupValues
                ?.let { matchGroups ->
                    when (matchGroups.size) {
                        2 -> OffLine(matchGroups[1], null)
                        3 -> OffLine(matchGroups[1], matchGroups[2])
                        else -> throw IllegalStateException("can't parse classRoom name ${raw}")
                    }
                }
                ?: throw IllegalStateException("can't parse classRoom name ${raw}")

        }
        // companion object ends
    }

}
