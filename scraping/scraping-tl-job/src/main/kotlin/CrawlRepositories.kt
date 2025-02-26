package io.gitp.ylfs.scraping.scraping_tl_job

import io.gitp.ylfs.entity.enums.GradeEvalMethod
import io.gitp.ylfs.entity.enums.Language
import io.gitp.ylfs.entity.enums.LectureType
import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.entity.model.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.ResultSet
import java.time.Year

private fun <T : Any> String.execAndMap(transform: (ResultSet) -> T): List<T> {
    val result = mutableListOf<T>()
    TransactionManager.current().exec(this) { rs ->
        while (rs.next()) {
            result += transform(rs)
        }
    }
    return result
}

class CrawledLectureViewRepository(
    private val db: Database
) {
    fun parseLanguage(code: Int?): Language {
        return when (code) {
            null -> Language.KOREAN
            10 -> Language.ENGLISH
            20 -> Language.ENGLISH
            else -> throw IllegalArgumentException("unexpected langauge code $code")
        }
    }

    fun parseGradeEvalMethodCode(str: String?): GradeEvalMethod {
        return when (str) {
            "상대평가" -> GradeEvalMethod.RELATIVE
            "절대평가" -> GradeEvalMethod.ABSOLUTE
            "P/NP" -> GradeEvalMethod.P_OR_NP
            null -> GradeEvalMethod.NONE
            else -> throw IllegalArgumentException("unexpected garde eval method code $str")
        }
    }


    val findAllQuery = """
        SELECT
            # @formatter:off
            job.year,
            job.semester,
            college.name,
            college.code,
            dpt.name,
            dpt.code,

            lecture.main_code,
            lecture.class_code,
            lecture.sub_code,
            lecture.name,
            lecture.eng_name,

            lecture.professors,
            lecture.lecture_type,
            lecture.locations,
            lecture.schedules,
            lecture.grades,
            lecture.credit,
            lecture.grade_eval_method,
            lecture.language_code
            # @formatter:on

        FROM dpt_resp_view AS dpt
             JOIN crawl_job AS job ON job.crawl_job_id = dpt.crawl_job_id
             JOIN college_resp_view AS college ON dpt.college_code = college.code AND college.crawl_job_id = dpt.crawl_job_id
             JOIN lecture_resp_view AS lecture ON lecture.dpt_code = dpt.code AND lecture.crawl_job_id = dpt.crawl_job_id
        WHERE college.code NOT IN ('s11006', 's11007', 's11008', 's11009', 's11003', 's11004', 's11005')
        ;
    """.trimIndent()

    fun findAll(): List<Lecture> = transaction(db) {

        findAllQuery.execAndMap { result ->
            val yearRaw = result.getString("year")
            val semesterRaw = result.getString("semester")

            val collegeNameRaw = result.getString("college.name")
            val collegeCodeRaw = result.getString("college.code")

            val dptNameRaw = result.getString("dpt.name")
            val dptCodeRaw = result.getString("dpt.code")

            val mainCodeRaw = result.getString("lecture.main_code")
            val classCodeRaw = result.getString("lecture.class_code")
            val subCodeRaw = result.getString("lecture.sub_code")

            val lectureNameRaw: String? = result.getString("lecture.name")
            val lectureEngNameRaw: String? = result.getString("lecture.eng_name")
            val lectureTypeRaw: String? = result.getString("lecture.lecture_type")

            val locationsRaw = result.getString("lecture.locations")
            val schedulesRaw = result.getString("lecture.schedules")

            val professorsRaw: String = result.getString("lecture.professors") ?: ""
            val gradesRaw: String = result.getString("lecture.grades").let { if (result.wasNull()) "0" else it }
            val creditRaw = result.getInt("lecture.credit")
            val gradeEvalMethodRaw = result.getString("lecture.grade_eval_method").let { if (result.wasNull()) null else it }
            val languageCodeRaw: Int? = result.getInt("lecture.language_code").let { if (result.wasNull()) null else it }

            // processed
            val year = Year.parse(yearRaw.slice(0..3))
            val semester = Semester.valueOf(semesterRaw)
            val lectureType = LectureType.parse(lectureTypeRaw)
            val gradeEvalMethod = parseGradeEvalMethodCode(gradeEvalMethodRaw)
            val language = parseLanguage(languageCodeRaw)
            val professors = professorsRaw.split(",")
            val lectureName = lectureNameRaw ?: lectureEngNameRaw
            ?: throw IllegalStateException("both of lecture name and lecture english name can't be null")

            // val locAndScheds: List<LocAndSched> = LocationScheduleParser.parse(locationsRaw, schedulesRaw)
            val locAndScheds: List<LocAndSched> = emptyList()

            val grades: List<Int> = gradesRaw.split(",").map { it.toInt() }
                .let { gradeList ->
                    if (gradeList.size == 1 && gradeList.first() == 0)
                        emptyList()
                    else
                        gradeList
                }

            val term = Term(
                year = year,
                semester = semester,
            )
            val college = College(
                code = collegeCodeRaw,
                name = collegeNameRaw,
                term = term
            )

            val dpt = Dpt(
                college = college,
                code = dptCodeRaw,
                name = dptNameRaw
            )

            val subclassLocSched = SubclassLocSched(
                subCode = subCodeRaw,
                locAndScheds = locAndScheds
            )

            val lecture = Lecture(
                term = term,
                mainCode = mainCodeRaw,
                classCode = classCodeRaw,
                dptAndLectureType = mapOf(dpt to lectureType),
                subclassLocScheds = listOf(subclassLocSched),
                professors = professors,
                name = lectureName,
                grades = grades,
                credit = creditRaw,
                gradeEvalMethod = gradeEvalMethod,
                language = language,
                descriptions = ""
            )
            return@execAndMap lecture
        }
    }
}

// fun main() {
//     val crawlDB: Database = Database.connect(
//         url = "jdbc:mysql://43.202.5.149:3306/crawl",
//         driver = "com.mysql.cj.jdbc.Driver",
//         user = "root",
//         password = "root_pass"
//     )
//     val db: Database = Database.connect(
//         url = "jdbc:mysql://43.202.5.149:3306/ylfs",
//         driver = "com.mysql.cj.jdbc.Driver",
//         user = "root",
//         password = "root_pass"
//     )
//
//     val crawledLectureRepo = CrawledLectureViewRepository(crawlDB)
//     val lectures = crawledLectureRepo.findAll().onEach { println(it) }
//
//     transaction(db) {
//         exec("SET FOREIGN_KEY_CHECKS = 0;")
//         exec("truncate term;")
//         exec("truncate college;")
//         exec("truncate dpt;")
//         exec("truncate dpt_lecture;")
//         exec("truncate lecture;")
//         exec("SET FOREIGN_KEY_CHECKS = 1;")
//     }
//
//     val termRepo = TermRepository(db)
//     val collegeRepo = CollegeRepostitory(db, termRepo)
//     val dptRepo = DptRepository(db, collegeRepo)
//     val lectureRepo = LectureRepository(db, dptRepo, termRepo)
//
//     runBlocking {
//         lectures
//             .chunked(30)
//             .map { lectureChunk ->
//                 launch { lectureChunk.map { lectureRepo.insertIfNotExists(it) } }
//             }
//         // lectures.onEach { lecture ->
//         //     launch { lectureRepo.insertLecture(lecture) }
//         //     delay(1000)
//         // }
//     }
// }
