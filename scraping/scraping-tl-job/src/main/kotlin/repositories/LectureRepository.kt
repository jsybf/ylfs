package io.gitp.ylfs.scraping.scraping_tl_job.repositories

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.lecture.LectureDto
import io.gitp.ylfs.scraping.scraping_tl_job.tables.LectureTbl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year

class LectureRepository(
    private val db: Database
) {
    fun getIdOrNull(year: Year, semester: Semester, mainCode: String, classCode: String): Int? = transaction(db) {
        return@transaction LectureTbl
            .select(LectureTbl.id)
            .where {
                (LectureTbl.year eq year) and (LectureTbl.semester eq semester) and (LectureTbl.mainCode eq mainCode) and (LectureTbl.classCode eq classCode)
            }
            .limit(1)
            .singleOrNull()
            ?.get(LectureTbl.id)
            ?.value
    }


    fun insertIfNotExists(lectureDto: LectureDto): Int = transaction(db) {
        getIdOrNull(lectureDto.year, lectureDto.semester, lectureDto.mainCode, lectureDto.classCode)?.let { id -> return@transaction id }

        return@transaction LectureTbl.insertAndGetId {
            it[LectureTbl.year] = lectureDto.year
            it[LectureTbl.semester] = lectureDto.semester
            it[LectureTbl.mainCode] = lectureDto.mainCode
            it[LectureTbl.classCode] = lectureDto.classCode
            it[LectureTbl.credit] = lectureDto.credit
            it[LectureTbl.name] = lectureDto.name
            it[LectureTbl.professors] = lectureDto.professors.joinToString(",")
            it[LectureTbl.grades] = lectureDto.grades.joinToString(",")
            it[LectureTbl.gradeEvalMethod] = lectureDto.gradeEvalMethod
            it[LectureTbl.language] = lectureDto.language
        }.value
    }
}