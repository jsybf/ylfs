package io.gitp.ysfl.client.deserializer

import io.gitp.ysfl.client.PairList
import io.gitp.ysfl.client.response.LocationUnion
import io.gitp.ysfl.client.response.Schedule
import java.time.DayOfWeek

/**
 * 연세대학교 수강편람사이트의 프런트와 서버 ajax 요청을 모방하는 방식으로 스크래핑을 하는 데
 * 하.... 교실과 시간표 응답이 일정한 형식을 띄는 것이 아니라 좇같이 쏴줌. 실제로 사이트에서도 줫같이 생겨먹은 그대로 사용자한테
 * 보여준다.
 *
 * 그래도 일단은 패턴이 있음
 *
 * '(' , ')', '/'로 강의실과 시간표를 각각 청킹하는 데 이 청킹이 강의실과 시간표에서 같은 형태로 나타난다.
 * 그리고 각각의 청크(강의실 청크 <->시간표 청크)가 매핑됨
 *
 * ```
 * 교405(교405)   화7,8,목7(목8)
 * 공C033(공C033)   화6(화7,8,9,10)
 * 동영상콘텐츠/상본B110/	월5,6/수5/(수6)
 * ```
 * 예를들어 3번째 케이스를 보면 아래와 같은 이런느낌으로 매핑이됨
 *
 * ```
 *  동영상콘텐츠    <->        월5,6
 *  상본B110        <->       수5
 *  상본B103        <->       (수6)
 * ```
 *
 *  아 좋다. 그럼 '(' , ')', '/'기준으로 토크나이징??을 하면 되지 않을까 하는 데 아니다.
 *  아래와 같이 괄호가 부가정보를 추가하기 위해 쓰인 경우들이 운동수업과 동영상수업에 있다.
 *
 *. 스포츠(태권도장)	(화1,2)
 *  동영상(중복수강불가)	금7,8,9
 *
 *  동영상수업에서는 동영상(중복수강불가)가 유일하고
 *  운동수업에서는 존나많다. ex) (스포츠(다목적실3)), (체조장(체308)), 수영장(신촌)
 *
 *  따라서 토크나이징을 쉽게하기 위해서 저런 꼴들을
 *
 * ```
 *  동영상(중복수강불가) -> 동영상_중복수강불가
 *  (스포츠(다목적실))   -> (스포츠_다목적실)
 *  수영장(신촌)          -> 수영장_신촌
 * ```
 *
 *  이런 꼴로 변환후 파싱
 *
 *  다행이 부가정보를 괄호로 포한하는 꼴 즉 ~~1~~(~~2~~) 에서 ~~1~~은 정해져있다. [replaceFuckingLectureForm] 코드 참고
 *
 *  종합해서 시간표와 교실을 매칭하는 과정은
 *  1. [replaceFuckingLectureForm]으로 좇같은 교실 형태들을 교정해주고
 *  2.  [associateLocAndSched]으로 교실과 시간표 매핑
 *  3. [LocationParser.parse] 와 [ScheduleParser.parse]으로 시간표와 교실을 객체로 매핑
 *
 *  이렇게 3단계로 이루어짐
 *
 */
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

    internal fun associateLocAndSched(location: String, schedule: String): PairList<String, String> {
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
