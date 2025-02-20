package io.gitp.ylfs.scraping.scraping_tl_job.parsers

import io.gitp.ylfs.entity.enums.Day
import io.gitp.ylfs.entity.model.LocAndSched
import io.gitp.ylfs.entity.model.LocationUnion
import io.gitp.ylfs.entity.model.Period

internal object LocationScheduleParser {

    internal fun parse(locations: String, schedules: String): List<LocAndSched> {
        return associateLocAndSched(locations, schedules)
            .flatMap { (location, schedule) ->
                val locationParsed: LocationUnion = LocationParser.parse(location)
                val scheduleParsedList: List<Period> = ScheduleParser.parse(schedule)
                scheduleParsedList.map { LocAndSched(it, locationParsed) }
            }
    }

    internal fun associateLocAndSched(location: String, schedule: String): List<Pair<String, String>> {
        val refinedLocation = replaceFuckingLectureForm(location)

        val locationSplitted = splitLogical(refinedLocation)
        val scheduleSplitted = splitLogical(schedule)

        require(locationSplitted.size == scheduleSplitted.size)

        return locationSplitted.zip(scheduleSplitted)
    }


    private val fuckingLecturePattern = Regex("""(?<building>[가-힣A-Z0-9]+)\((?<address>[가-힣A-Z0-9]+)_?\)""")
    private fun replaceFuckingLectureForm(str: String): String {
        return fuckingLecturePattern.replace(str) { matchResult: MatchResult ->
            when (matchResult.groupValues[1]) {
                "스포츠", "무용실", "체조장", "테니스장", "동영상", "운동장", "수영장" -> "${matchResult.groupValues[1]}_${matchResult.groupValues[2]}"
                else -> matchResult.groupValues[0]
            }
        }
    }


    private fun splitLogical(str: String): List<String> {
        val scheduleList = mutableListOf<String>()

        val stringBuilder = StringBuilder()
        str.forEach { c ->
            when (c) {
                ')', '(', '/' -> {
                    val schedule: String = stringBuilder.toString()
                    stringBuilder.clear()
                    scheduleList.add(schedule)
                }
                else -> {
                    stringBuilder.append(c)
                }
            }
        }
        scheduleList.add(stringBuilder.toString())
        return scheduleList.filter { it != "" }
    }
}


private object LocationParser {
    private val buildingNames = listOf(
        // normal building names
        listOf(
            "외", "위", "상본", "상별", "과", "공A", "공B", "공C", "공D", "연", "빌", "백", "삼", "교", "광", "음",
            "새천", "이윤재", "대별", "경영", "원", "첨", "루", "공학원", "신", "중입자", "IBS", "KLI", "성"
        ),
        // sport building name
        listOf(
            "스포츠", "체조장", "테니스장", "골프장", "체육관", "스포츠", "체", "볼링장", "야구장", "무용실", "운동장", "수영장"
        ),
        // fucking edge cases
        listOf(
            "석산홀세미나", "윤주용홀", "제1강의실", "제2강의실"
        )
    ).flatten()


    fun parse(location: String): LocationUnion {
        val buildingName: String? = buildingNames.find { buildingName -> location.startsWith(buildingName) }

        if (buildingName == null) {
            return when (location) {
                "동영상콘텐츠" -> LocationUnion.Online(true)
                "실시간온라인" -> LocationUnion.RealTimeOnline
                "동영상_중복수강불가" -> LocationUnion.Online(false)
                else -> throw IllegalArgumentException("unexpected online location : $location")
            }
        }

        val address = location.removePrefix(buildingName).removePrefix("_").removeSuffix("_")
        return LocationUnion.OffLine(buildingName, address)
    }
}

private object ScheduleParser {
    private val schedulePattern = Regex("""(?<day>[월화수목금토일])(?<times>(-?\d{1,2},?)+)""")

    fun parse(scheduleRaw: String): List<Period> {
        val matchGroups = schedulePattern.findAll(scheduleRaw).map { it.groupValues }.toList()
        val schedules = matchGroups.map { mathGroupValue: List<String> ->
            val day = mathGroupValue[1][0]
            val times = mathGroupValue[2].split(",").filterNot(CharSequence::isEmpty).map(String::toInt)
            Period(hangulToDay(day), times)
        }
        return schedules
    }

    private fun hangulToDay(hangulDay: Char): Day {
        return when (hangulDay) {
            '월' -> Day.MON
            '화' -> Day.TUE
            '수' -> Day.WEN
            '목' -> Day.THU
            '금' -> Day.FRI
            '토' -> Day.SAT
            '일' -> Day.SUN
            else -> throw IllegalArgumentException("unexpected hangul day: [${hangulDay}]")
        }
    }
}
