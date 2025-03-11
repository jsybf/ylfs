package io.gitp.ylfs.scraping.scraping_tl_job.tables

import io.gitp.ylfs.entity.enums.GradeEvalMethod
import io.gitp.ylfs.entity.enums.Language
import io.gitp.ylfs.entity.enums.LectureType
import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.entity.model.LocAndSched
import io.gitp.ylfs.scraping.scraping_tl_job.tables.types.year
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.UIntIdTable
import org.jetbrains.exposed.sql.json.json

object LectureProcessTbl : UIntIdTable("lecture_process_tbl", "lecture_process_tbl_id") {
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

