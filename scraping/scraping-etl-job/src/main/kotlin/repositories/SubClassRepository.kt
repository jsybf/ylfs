package io.gitp.ylfs.scraping.scraping_tl_job.repositories

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.tables.LectureTbl
import io.gitp.ylfs.scraping.scraping_tl_job.tables.SubClassTbl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year

class SubClassRepository(
    private val db: Database
) {
    fun getIdOrNull(
        year: Year,
        semester: Semester,
        mainCode: String,
        classCode: String,
        subCode: String
    ): Int? = transaction(db) {
        return@transaction (SubClassTbl innerJoin LectureTbl)
            .select(SubClassTbl.id)
            .where {
                (SubClassTbl.subCode eq subCode) and
                        (LectureTbl.year eq year) and
                        (LectureTbl.semester eq semester) and
                        (LectureTbl.mainCode eq mainCode) and
                        (LectureTbl.classCode eq classCode)
            }
            .limit(1)
            .singleOrNull()
            ?.get(SubClassTbl.id)
            ?.value
    }

    fun getLectureIdOrNull(year: Year, semester: Semester, mainCode: String, classCode: String): Int? = transaction(db) {
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

    fun insertIfNotExists(
        year: Year,
        semester: Semester,
        mainCode: String,
        classCode: String,
        subCode: String

    ): Int = transaction(db) {
        getIdOrNull(year, semester, mainCode, classCode, subCode)?.let { id -> return@transaction id }

        return@transaction SubClassTbl.insertAndGetId {
            it[SubClassTbl.subCode] = subCode
            it[SubClassTbl.lectureId] = getLectureIdOrNull(year, semester, mainCode, classCode)!!
        }.value

    }
}