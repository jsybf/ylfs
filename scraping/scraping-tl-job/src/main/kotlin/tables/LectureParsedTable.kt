package io.gitp.ylfs.scraping.scraping_tl_job.tables

import io.gitp.ylfs.entity.enums.GradeEvalMethod
import io.gitp.ylfs.entity.enums.Language
import io.gitp.ylfs.entity.enums.LectureType
import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.entity.model.LocAndSched
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.lecture.LectureDto
import io.gitp.ylfs.scraping.scraping_tl_job.tables.types.year
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.UIntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.transactions.transaction

object LectureParsedTable : UIntIdTable("lecture_process_tbl", "lecture_process_tbl_id") {
    val semester = enumerationByName<Semester>("semester", 7)
    val year = year("year")

    val mainCode = char("main_code", 7)
    val classCode = char("class_code", 2)
    val subCode = char("sub_code", 2)

    val name = varchar("name", 100)

    val professors = varchar("professors", 255)

    val grades = varchar("grades", 20)

    val credit = decimal("credit", 3, 1)
    val gradeEvalMethod = enumerationByName<GradeEvalMethod>("grade_eval_method", 10)
    val language = enumerationByName<Language>("language", 10)
    val description = text("description").nullable()
    val lectureType = enumerationByName<LectureType>("lecture_type", 10)


    val dptCode = DptTbl.varchar("dpt_code", 5)
    val locAndScheds = json<List<LocAndSched>>("loc_sched", Json.Default)
}

class LectureParsedRepository(
    private val db: Database
) {
    fun batchInsert(lectureDtoList: List<LectureDto>) = transaction(db) {
        LectureParsedTable.batchInsert(lectureDtoList) { lectureDto ->
            this[LectureParsedTable.year] = lectureDto.year
            this[LectureParsedTable.semester] = lectureDto.semester

            this[LectureParsedTable.mainCode] = lectureDto.mainCode
            this[LectureParsedTable.classCode] = lectureDto.classCode
            this[LectureParsedTable.subCode] = lectureDto.subCode


            this[LectureParsedTable.credit] = lectureDto.credit

            this[LectureParsedTable.name] = lectureDto.name

            this[LectureParsedTable.professors] = lectureDto.professors.joinToString(",")

            this[LectureParsedTable.lectureType] = lectureDto.lectureType
            this[LectureParsedTable.grades] = lectureDto.grades.joinToString(",")
            this[LectureParsedTable.credit] = lectureDto.credit
            this[LectureParsedTable.gradeEvalMethod] = lectureDto.gradeEvalMethod
            this[LectureParsedTable.language] = lectureDto.language
            this[LectureParsedTable.description] = null
            this[LectureParsedTable.dptCode] = lectureDto.dptCode
            this[LectureParsedTable.locAndScheds] = lectureDto.locAndScheds

        }
    }
}