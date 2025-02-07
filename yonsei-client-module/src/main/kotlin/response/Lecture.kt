package io.gitp.ysfl.client.response

import io.gitp.ysfl.client.deserializer.ClassroomUnion
import io.gitp.ysfl.client.deserializer.LectureDeserializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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


fun main() {
    val decoded: Lecture = Json.decodeFromString<Lecture>(sampleJson)
    println(decoded)
}

private val sampleJson = """
{
  "lawscSubjcgpNm": null,
  "srclnLctreLangDivCd": "10",
  "coprtEstblYn": "1",
  "smtDivCd": "10",
  "lessnSessnDivCd": "A",
  "lctreTimeNm": "월1,2/수2",
  "lawscSubjcFldNm": null,
  "corseDvclsNo": "01",
  "hy": "1,2",
  "subsrtDivCd": "F3",
  "subjtnb": "ECO1101",
  "experPrctsAmt": 0,
  "subjtSbtlNm": null,
  "subjtClNm": "블랜디드(동영상) ",
  "campsDivNm": "신촌",
  "syllaUnregTrgetDivCd": "0",
  "subjtChngGudncDivCdTm": null,
  "onppsPrttnAmt": 0,
  "subjtChngGudncDivCdPl": null,
  "gradeEvlMthdDivNm": "절대평가",
  "cdt": 3,
  "srclnLctreYn": "1",
  "rcognHrs": 3,
  "cgprfNm": "지창구",
  "subjtChngGudncDivCdPr": null,
  "timtbDplctPermKindCd": null,
  "atntnMattrDesc": "UIC First",
  "usubjtnb": "B00019",
  "lctreTimeEngNm": "Mon1,2/Wed2",
  "rmvlcYn": "0",
  "excstPercpFg": "1",
  "subjtNm": "경제수학(1)",
  "lecrmNm": "상본115/동영상(중복수강불가)",
  "rmvlcYnNm": " ",
  "medcHyLisup": null,
  "gradeEvlMthdDivCd": "1",
  "estblDeprtCd": "0201",
  "subjtNm2": "경제수학(1)",
  "syy": "2025",
  "prctsCorseDvclsNo": "00",
  "campsBusnsCd": "s1",
  "cgprfEngNm": "Chi Chang-Koo",
  "syySmtDivNm": "2025-1학기",
  "estblDeprtOrd": 950,
  "subjtnbCorsePrcts": "ECO1101-01-00",
  "subjtUnitVal": "1000",
  "srclnLctreLangDivNm": "영어",
  "cgprfNndsYn": "0",
  "estblDeprtNm": "상경대학 경제학전공",
  "subjtEngNm": "MATHEMATICS FOR ECONOMICS I",
  "orgSysinstDivCd": "H1",
  "lecrmEngNm": "DWHM115/Pre-recorded lecture(Unable to take other class)",
  "lessnSessnDivNm": "학기",
  "subjtSbtlEngNm": null,
  "attflUuid": null,
  "cmptPrctsAmt": 0,
  "tmtcYn": "0",
  "subsrtDivNm": "전기",
  "sysinstDivCd": "H1",
  "lawscSubjcChrtzNm": null
}
""".trimIndent()