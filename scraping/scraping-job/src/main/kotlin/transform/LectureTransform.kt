package io.gitp.yfls.scarping.job.file.transform

import io.gitp.yfls.scarping.job.file.YearSerializer
import io.gitp.yfls.scarping.job.file.request.LectureResponse
import io.gitp.ylfs.entity.enums.Day
import io.gitp.ylfs.entity.enums.GradeEvalMethod
import io.gitp.ylfs.entity.enums.Language
import io.gitp.ylfs.entity.enums.LectureType
import io.gitp.ylfs.entity.model.LocAndSched
import io.gitp.ylfs.entity.model.LocationUnion
import io.gitp.ylfs.entity.model.Period
import io.gitp.ylfs.entity.enums.Semester
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Year

@Serializable
data class LectureVo(
    @Serializable(with = YearSerializer::class)
    val year: Year,
    val semester: Semester,

    val collegeCode: String,
    val dptCode: String,

    val mainCode: String,
    val classCode: String,
    val subCode: String,

    val name: String,
    val professors: List<String>,

    val grades: List<Int>,
    val credit: Float,
    val gradeEvalMethod: GradeEvalMethod,
    val language: Language,

    val lectureType: LectureType,
    val locAndSchedList: List<LocAndSched>,
)

fun transfromLectureResp(lecture: LectureResponse): List<LectureVo> {
    val respBodyList: JsonArray = lecture.resp["dsSles251"]!!.jsonArray
    return respBodyList
        .map { it.jsonObject }
        .map { respBody ->
            val year: Year = lecture.request.year
            val semester: Semester = lecture.request.semester

            val collegeCode: String = lecture.request.collegeCode
            val dpeCode: String = lecture.request.dptCode

            val mainCode: String = respBody["subjtnb"]!!.jsonPrimitive.content
            val classCode: String = respBody["corseDvclsNo"]!!.jsonPrimitive.content
            val subCode: String = respBody["prctsCorseDvclsNo"]!!.jsonPrimitive.content

            val name: String = respBody["subjtNm"]!!.jsonPrimitive.content
            val credit: String = respBody["cdt"]!!.jsonPrimitive.content
            val grades: String = respBody["hy"]!!.jsonPrimitive.content
            val professors: String = respBody["cgprfNm"]!!.jsonPrimitive.content


            val gradeEvalMethod: String? = respBody["gradeEvlMthdDivNm"]!!.jsonPrimitive.contentOrNull
            val languageCode: Int? = respBody["srclnLctreLangDivCd"]!!.jsonPrimitive.intOrNull

            val lectureType: String = respBody["subsrtDivNm"]!!.jsonPrimitive.content
            val loc: String? = respBody["lecrmNm"]!!.jsonPrimitive.contentOrNull
            val sched: String = respBody["lctreTimeNm"]!!.jsonPrimitive.content

            LectureVo(
                year = year,
                semester = semester,

                collegeCode = collegeCode,
                dptCode = dpeCode,

                mainCode = mainCode,
                classCode = classCode,
                subCode = subCode,

                name = name,
                credit = credit.toFloat(),
                professors = LectureParser.parseProfessors(professors),
                grades = LectureParser.parseGrades(grades),
                gradeEvalMethod = LectureParser.parseGradeEvalMethod(gradeEvalMethod),
                language = LectureParser.parseLanguageCode(languageCode),
                lectureType = LectureType.parse(lectureType),
                locAndSchedList = LectureParser.parselocAnsSched(loc, sched)
            )
        }
}


object LectureParser {
    fun parseGrades(raw: String): List<Int> = raw.split(",").map { it.toInt() }
    fun parseCredit(raw: String): BigDecimal = BigDecimal(raw)
    fun parseProfessors(raw: String): List<String> = raw.split(",")
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

    fun parselocAnsSched(loc: String?, sched: String): List<LocAndSched> {
        return LocationScheduleParser.parse(loc, sched)
    }
}

internal object LocationScheduleParser {

    internal fun parse(locations: String?, schedules: String?): List<LocAndSched> {
        return when {
            (locations == null && schedules == null) -> emptyList()
            (locations != null && schedules == null) -> throw IllegalStateException("location or scheduels should be all null or non not. one of them can't be null.  locations:${locations} schedules:${schedules}")
            (locations == null && schedules != null) -> ScheduleParser.parse(schedules).map { LocAndSched(it, LocationUnion.UnKnown) }
            else -> {
                associateLocAndSched(locations!!, schedules!!)
                    .flatMap { (location, schedule) ->
                        val locationParsed: LocationUnion = LocationParser.parse(location)
                        val scheduleParsedList: List<Period> = ScheduleParser.parse(schedule)
                        scheduleParsedList.map { LocAndSched(it, locationParsed) }
                    }
            }
        }
    }

    private fun associateLocAndSched(location: String, schedule: String): List<Pair<String, String>> {
        val refinedLocation = replaceFuckingLectureForm(location.replace("_", ""))

        val locationSplitted = splitLogical(refinedLocation)
        val scheduleSplitted = splitLogical(schedule)

        require(locationSplitted.size == scheduleSplitted.size)

        return locationSplitted.zip(scheduleSplitted)
    }


    private val fuckingLecturePattern = Regex("""(?<building>[가-힣A-Z0-9]+)\((?<address>[가-힣A-Z0-9]+)\)""")
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

    object LocationParser {
        private val logger = LoggerFactory.getLogger(LocationParser::class.java)
        private val buildingNames = listOf(
            // normal building names
            listOf(
                "외", "위", "상본", "상별", "과", "공A", "공B", "공C", "공D", "연", "빌", "백", "삼", "교", "광", "음",
                "새천", "이윤재", "대별", "경영", "원", "첨", "루", "공학원", "신", "중입자", "IBS", "KLI", "성", "유", "I자A", "아"
            ),
            // sport building name
            listOf(
                "스포츠", "체조장", "테니스장", "골프장", "체육관", "스포츠", "체", "볼링장", "야구장", "무용실", "운동장", "수영장"
            ),
            // fucking edge cases
            listOf(
                "석산홀세미나", "윤주용홀", "제1강의실", "제2강의실", "선수기숙사 트레이닝실", "미우", "김대중도서관3층", "의사학과 자료실", "그룹토의실 4번", "의사학과 자료실", "의대131호"
            )
        ).flatten()


        internal fun parse(location: String?): LocationUnion {
            if (location == null) return LocationUnion.UnKnown
            val buildingName: String? = buildingNames.find { buildingName -> location.startsWith(buildingName) }
            if (buildingName != null) {
                val address = location.removePrefix(buildingName).removePrefix("_").removeSuffix("_")
                return LocationUnion.OffLine(buildingName, address)
            }

            return when (location) {
                "동영상콘텐츠" -> LocationUnion.Online(true)
                "실시간온라인" -> LocationUnion.RealTimeOnline
                "동영상_중복수강불가" -> LocationUnion.Online(false)
                else -> {
                    return LocationUnion.OffLine(location, null).also { logger.debug("can't parse building name and address from [${location}]. just return {}", it) }
                }
            }

        }
    }

    object ScheduleParser {
        private val schedulePattern = Regex("""(?<day>[월화수목금토일])(?<times>(-?\d{1,2},?)+)""")

        internal fun parse(scheduleRaw: String): List<Period> {
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
}