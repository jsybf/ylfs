package io.gitp.ylfs.scraping.scraping_tl_job.repositories

import io.gitp.ylfs.entity.enums.LectureType
import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year

class DptLectureRepository(
    private val db: Database
) {
    fun getIdOrNull(year: Year, semester: Semester, mainCode: String, classCode: String, dptCode: String): Int? = transaction(db) {
        return@transaction (DptLectureTbl innerJoin DptTbl innerJoin LectureTbl)
            .select(DptLectureTbl.id)
            .where {
                (LectureTbl.year eq year) and
                        (LectureTbl.semester eq semester) and
                        (LectureTbl.mainCode eq mainCode) and
                        (LectureTbl.classCode eq classCode) and
                        (DptTbl.code eq dptCode)
            }
            .limit(1)
            .singleOrNull()
            ?.get(DptLectureTbl.id)
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

    fun getDptIdOrNull(year: Year, semester: Semester, dptCode: String): Int? = transaction(db) {
        return@transaction (DptTbl innerJoin CollegeTbl innerJoin TermTbl)
            .select(DptTbl.id)
            .where {
                (TermTbl.year eq year) and (TermTbl.semester eq semester) and (DptTbl.code eq dptCode)
            }
            .limit(1)
            .singleOrNull()
            ?.get(DptTbl.id)
            ?.value
    }

    fun insertIfNotExists(year: Year, semester: Semester, mainCode: String, classCode: String, dptCode: String, lectureType: LectureType): Int =
        transaction(db) {
            getIdOrNull(year, semester, mainCode, classCode, dptCode)?.let { id -> return@transaction id }

            return@transaction DptLectureTbl.insertAndGetId {
                it[DptLectureTbl.dptId] = getDptIdOrNull(year, semester, dptCode)!!
                it[DptLectureTbl.lectureId] = getLectureIdOrNull(year, semester, mainCode, classCode)!!
                it[DptLectureTbl.lectureType] = lectureType
            }.value

        }
}