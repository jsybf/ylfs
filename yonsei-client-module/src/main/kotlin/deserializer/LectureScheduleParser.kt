package io.gitp.ysfl.client.deserializer

import java.time.DayOfWeek

internal class LectureScheduleParser {
    private val dayDelimiter = Regex("""[/,)(]""")
    private val parseSchedule = Regex("""(?<day>[월화수목금토일])(?<times>(-?\d{1,2}\s*)+)""")

    public fun parseSchedule(scheduleRaw: String): Map<Char, List<Int>> {
        val whiteSpaceSperated = scheduleRaw.replace(dayDelimiter, " ")

        val result: Map<Char, List<Int>> = parseSchedule
            .findAll(whiteSpaceSperated)
            .groupBy(
                keySelector = { it.groups["day"]!!.value[0].toChar() },
                valueTransform = { it.groups["times"]!!.value.trim().split(" ").map { n -> n.toInt() } }
            )
            .mapValues { (_, period: List<List<Int>>) -> period.flatten() }
        return result
    }

    private fun hangulToJavaDay(hangulDay: Char): DayOfWeek {
        return when (hangulDay) {
            '월' -> DayOfWeek.MONDAY
            '화' -> DayOfWeek.TUESDAY
            '수' -> DayOfWeek.WEDNESDAY
            '목' -> DayOfWeek.THURSDAY
            '금' -> DayOfWeek.FRIDAY
            '토' -> DayOfWeek.SATURDAY
            '일' -> DayOfWeek.SUNDAY
            else -> throw IllegalArgumentException("unexpected hangul day: [${hangulDay}]")
        }
    }
}