package io.gitp.ylfs.scraping.scraping_tl_job.tables

import io.gitp.ylfs.entity.enums.GradeEvalMethod
import io.gitp.ylfs.entity.enums.Language
import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.tables.types.year
import org.jetbrains.exposed.dao.id.IntIdTable

object LectureTbl : IntIdTable("lecture", "lecture_id") {
    val semester = enumerationByName<Semester>("semester", 7)
    val year = year("year")

    val mainCode = char("main_code", 7)
    val classCode = char("class_code", 2)

    val name = varchar("name", 100)

    val professors = varchar("professors", 255)

    val grades = varchar("grades", 20)

    val credit = decimal("credit", 2, 1)
    val gradeEvalMethod = enumerationByName<GradeEvalMethod>("grade_eval_method", 10)
    val language = enumerationByName<Language>("language", 10)
    val description = text("description").nullable()
}
