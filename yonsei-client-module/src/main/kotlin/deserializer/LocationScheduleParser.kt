package io.gitp.ysfl.client.deserializer

import io.gitp.ysfl.client.PairList
import io.gitp.ysfl.client.response.LocationUnion
import io.gitp.ysfl.client.response.Schedule
import java.time.DayOfWeek

internal object LocationScheduleParser {

    internal fun parse(locations: String, schedules: String): Map<Schedule, LocationUnion> {
        return associateLocAndSched(locations, schedules)
            .flatMap { (location, schedule) ->
                val locationParsed: LocationUnion = LocationParser.parse(location)
                val scheduleParsed: List<Schedule> = ScheduleParser.parse(schedule)
                scheduleParsed.map { Pair(it, locationParsed) }
            }
            .associate { it }
    }

    /**
     * 교실과 시간표를 매핑한다
     * "동영상(중복수강불가)", "수영장(신촌)" 와 같이 부가정보를 위해 '(', ')'가 쓰인 경우
     * "동영상_중복수강불가", "수영장_신촌"로 파싱하기 쉽게 바꾼다
     *
     * split하는 과정에서 '/', '(', ')'가 제외된다.
     * examples:
     * 교405(교405)   화7,8,목7(목8) -> {교405 | 화7,8,목7}, {교405 | 목8}
     * 상본112/동영상콘텐츠/동영상(중복수강불가)	화7/목8/목9 -> {상본112 | 화7}, {동영상콘텐츠| 목8}, {동영상(중복수강불가)| 목9}
     * 과326(과326)   화4,5,목4(목5) -> {과326 | 화4,5,목4 }, {과326  | 목5}
     * 공C033(공C033)   화6(화7,8,9,10) -> {공C033 | 화6}, {공C033 | 화7,8,9,10}
     * (수영장(신촌))   (월1,2) -> {수영장_신촌 | 월1,2 }
     */
    internal fun associateLocAndSched(location: String, schedule: String): PairList<String, String> {
        val refinedLocation = replaceFuckingLectureForm(location)

        val locationSplitted = splitLogical(refinedLocation)
        val scheduleSplitted = splitLogical(schedule)

        require(locationSplitted.size == scheduleSplitted.size)

        return locationSplitted.zip(scheduleSplitted)
    }


    /**
     * "동영상(중복수강불가)", "수영장(신촌)" 와 같이 부가정보를 위해 '(', ')'가 쓰인 경우
     * "동영상_중복수강불가", "수영장_신촌"로 파싱하기 쉽게 바꾼다
     */
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
                "실시간온라인" -> LocationUnion.RealTimeOnline()
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

    fun parse(scheduleRaw: String): List<Schedule> {
        val matchGroups = schedulePattern.findAll(scheduleRaw).map { it.groupValues }.toList()
        val schedules = matchGroups.map { mathGroupValue: List<String> ->
            val day = mathGroupValue[1][0]
            val times = mathGroupValue[2].split(",").filterNot(CharSequence::isEmpty).map(String::toInt)
            Schedule(hangulToJavaDay(day), times)
        }
        return schedules
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
