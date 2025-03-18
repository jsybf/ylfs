package io.gitp.ylfs.scraping.scraping_tl_job.jobs.lecture

import io.gitp.ylfs.entity.enums.Day
import io.gitp.ylfs.entity.enums.GradeEvalMethod
import io.gitp.ylfs.entity.enums.Language
import io.gitp.ylfs.entity.enums.LectureType
import io.gitp.ylfs.entity.model.LocAndSched
import io.gitp.ylfs.entity.model.LocationUnion
import io.gitp.ylfs.entity.model.Period
import kotlinx.serialization.json.*
import java.math.BigDecimal

internal object LocationScheduleParser {

    internal fun parse(locations: String?, schedules: String?): List<LocAndSched> {
        if (locations == null && schedules == null) return emptyList()
        if ((locations == null && schedules != null) || (locations != null && schedules == null)) throw IllegalStateException("fuck locations:${locations} schedules:${schedules}")
        return associateLocAndSched(locations!!, schedules!!)
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
            "새천", "이윤재", "대별", "경영", "원", "첨", "루", "공학원", "신", "중입자", "IBS", "KLI", "성", "유"
        ),
        // sport building name
        listOf(
            "스포츠", "체조장", "테니스장", "골프장", "체육관", "스포츠", "체", "볼링장", "야구장", "무용실", "운동장", "수영장"
        ),
        // fucking edge cases
        listOf(
            "석산홀세미나", "윤주용홀", "제1강의실", "제2강의실", "선수기숙사 트레이닝실", "미우"
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

object LectureRespParser {
    fun parse(lectureDto: LectureRespDto): List<LectureDto> {
        val lectureJsonArr = lectureDto.resp.jsonObject["dsSles251"]!!.jsonArray

        return lectureJsonArr
            .map { it.jsonObject }
            .map { lectureJson ->
                // 폐강된 수업은 필터링
                if (lectureJson["rmvlcYn"]!!.jsonPrimitive.intOrNull == 1) return@map null
                LectureDto(
                    year = lectureDto.year,
                    semester = lectureDto.semester,

                    collegeCode = lectureDto.collegeCode,
                    dptCode = lectureDto.dptCode,

                    mainCode = lectureJson["subjtnb"]!!.jsonPrimitive.content,
                    classCode = lectureJson["corseDvclsNo"]!!.jsonPrimitive.content,
                    subCode = lectureJson["prctsCorseDvclsNo"]!!.jsonPrimitive.content,

                    name = lectureJson["subjtNm"]!!.jsonPrimitive.content,
                    professors = parseProfessors(lectureJson["cgprfNm"]!!.jsonPrimitive.content),

                    grades = parseGrades(lectureJson["hy"]!!.jsonPrimitive.contentOrNull),

                    credit = lectureJson["cdt"]!!.jsonPrimitive.content.let { BigDecimal(it) },
                    gradeEvalMethod = parseGradeEvalMethod(lectureJson["gradeEvlMthdDivNm"]!!.jsonPrimitive.contentOrNull),
                    language = parseLanguageCode(lectureJson["srclnLctreLangDivCd"]!!.jsonPrimitive.intOrNull),

                    lectureType = LectureType.parse(lectureJson["subsrtDivNm"]!!.jsonPrimitive.contentOrNull),
                    locAndScheds = LocationScheduleParser.parse(lectureJson["lecrmNm"]!!.jsonPrimitive.contentOrNull, lectureJson["lctreTimeNm"]!!.jsonPrimitive.contentOrNull)
                )
            }
            .filterNotNull()
    }

    fun parseProfessors(str: String?): List<String> {
        return if (str == null) emptyList()
        else str.split(",")
    }

    fun parseGrades(str: String?): List<Int> {
        return if (str == null) emptyList()
        else str.split(",").map { it.toInt() }.sorted()
    }

    fun parseGradeEvalMethod(str: String?): GradeEvalMethod = when (str) {
        null -> GradeEvalMethod.NONE
        "P/NP" -> GradeEvalMethod.P_OR_NP
        "절대평가" -> GradeEvalMethod.ABSOLUTE
        "상대평가" -> GradeEvalMethod.RELATIVE
        else -> throw IllegalStateException("unexpected grade eval method:${str}")
    }

    fun parseLanguageCode(code: Int?) = when (code) {
        null -> Language.KOREAN
        10 -> Language.ENGLISH
        20 -> Language.ETC
        else -> throw IllegalStateException("unexpected language code:${code}")
    }

}
fun main() {
    val offlineStr = Json.encodeToString(LocationUnion.OffLine("building1", "address1")).also { println(it) }
    println(Json.decodeFromString<LocationUnion>(offlineStr))
    val realTimeStr = Json.encodeToString(LocationUnion.RealTimeOnline)
    println(Json.decodeFromString<LocationUnion>(realTimeStr))
}
