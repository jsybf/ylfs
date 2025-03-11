package io.gitp.ylfs.scraping.scraping_tl_job.repositories.response

import io.gitp.ylfs.scraping.scraping_tl_job.jobs.lecture.LectureDto
import io.gitp.ylfs.scraping.scraping_tl_job.tables.LectureProcessTbl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction

class LectureProcessRepository(
    private val db: Database
) {
    fun batchInsert(lectureDtoList: List<LectureDto>) = transaction(db) {
        LectureProcessTbl.batchInsert(lectureDtoList) { lectureDto ->
            this[LectureProcessTbl.year] = lectureDto.year
            this[LectureProcessTbl.semester] = lectureDto.semester

            this[LectureProcessTbl.mainCode] = lectureDto.mainCode
            this[LectureProcessTbl.classCode] = lectureDto.classCode
            this[LectureProcessTbl.subCode] = lectureDto.subCode


            this[LectureProcessTbl.credit] = lectureDto.credit

            this[LectureProcessTbl.name] = lectureDto.name

            this[LectureProcessTbl.professors] = lectureDto.professors.joinToString(",")

            this[LectureProcessTbl.lectureType] = lectureDto.lectureType
            this[LectureProcessTbl.grades] = lectureDto.grades.joinToString(",")
            this[LectureProcessTbl.credit] = lectureDto.credit
            this[LectureProcessTbl.gradeEvalMethod] = lectureDto.gradeEvalMethod
            this[LectureProcessTbl.language] = lectureDto.language
            this[LectureProcessTbl.description] = null
            this[LectureProcessTbl.dptCode] = lectureDto.dptCode
            this[LectureProcessTbl.locAndScheds] = lectureDto.locAndScheds

        }
    }
}