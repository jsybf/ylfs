package io.gitp.ylfs.parse_load_job

import io.gitp.ylfs.entity.model.College
import io.gitp.ylfs.entity.model.Dpt
import io.gitp.ylfs.entity.model.Lecture
import io.gitp.ylfs.entity.model.Term
import io.gitp.ylfs.parse_load_job.tables.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.coroutines.CoroutineContext


private suspend fun <T> suspendTx(
    db: Database, context: CoroutineContext, block: suspend Transaction.() -> T
) = newSuspendedTransaction(context, db) { block() }

class LectureRepository(
    private val db: Database,
    private val dptRepository: DptRepository,
    private val termRepository: TermRepository
) {
    suspend fun getIdOrNull(lecture: Lecture): Int? = suspendTx(db, Dispatchers.IO) {
        val termId: Int = termRepository.getIdOrNull(lecture.term) ?: return@suspendTx null

        val query: Query = LectureTbl.select(LectureTbl.id)
            .where { (LectureTbl.termId eq termId) and (LectureTbl.mainCode eq lecture.mainCode) and (LectureTbl.classCode eq lecture.classCode) }
            .limit(1)

        return@suspendTx query.singleOrNull()?.let { it[LectureTbl.id].value }
    }

    suspend fun insertIfNotExists(lecture: Lecture): Int = suspendTx(db, Dispatchers.IO) {

        lecture
            .dptAndLectureType
            .map { (dpt, _) -> dptRepository.insertIfNotExistsCascade(dpt) }

        getIdOrNull(lecture)?.let { lectureId ->


            return@suspendTx lectureId
        }

        val termId = termRepository.getIdOrNull(lecture.term)!!


        val id = LectureTbl.insertAndGetId {
            it[LectureTbl.termId] = termId

            it[LectureTbl.mainCode] = lecture.mainCode
            it[LectureTbl.classCode] = lecture.classCode

            it[LectureTbl.name] = lecture.name

            it[LectureTbl.professors] = lecture.professors.joinToString(",")

            it[LectureTbl.grades] = lecture.grades.joinToString(",")

            it[LectureTbl.credit] = lecture.credit
            it[LectureTbl.gradeEvalMethod] = lecture.gradeEvalMethod
            it[LectureTbl.language] = lecture.language
            it[LectureTbl.description] = lecture.descriptions
        }.value

        lecture
            .dptAndLectureType
            .onEach { (dpt, lectureType) ->
                val dptId = dptRepository.insertIfNotExistsCascade(dpt)
                DptLectureTbl.insert {
                    it[DptLectureTbl.lectureId] = id
                    it[DptLectureTbl.dptId] = dptId
                    it[DptLectureTbl.lectureType] = lectureType
                }
            }
        return@suspendTx id
    }
}

class DptLectureRepository(
    private val db: Database,
    private val dptRepo: DptRepository,
    private val termRepository: TermRepository
) {
    suspend fun getLectureIdOrNull(lecture: Lecture): Int? = suspendTx(db, Dispatchers.IO) {
        val termId: Int = termRepository.getIdOrNull(lecture.term) ?: return@suspendTx null

        val query: Query = LectureTbl.select(LectureTbl.id)
            .where { (LectureTbl.termId eq termId) and (LectureTbl.mainCode eq lecture.mainCode) and (LectureTbl.classCode eq lecture.classCode) }
            .limit(1)

        return@suspendTx query.singleOrNull()?.let { it[LectureTbl.id].value }
    }

    suspend fun getIdOrNull(dpt: Dpt, lecture: Lecture): Int? = suspendTx(db, Dispatchers.IO) {
        val dptId = dptRepo.getIdOrNull(dpt)
        val lectureId = getLectureIdOrNull(lecture)

        if (dptId == null && lectureId == null) return@suspendTx null

        val result = DptLectureTbl.select(DptLectureTbl.id)
            .where { (DptLectureTbl.dptId eq dptId) and (DptLectureTbl.lectureId eq lectureId) }
            .limit(1)

        return@suspendTx result.firstOrNull()?.let { it[DptTbl.id].value }
    }

    // suspend fun insertIfNotExistsCascade(dpt: Dpt, lecture: Lecture): Int = suspendTx(db, Dispatchers.IO) {
    //     return@suspendTx getLectureIdOrNull(dpt) ?: insertCollege(dpt)
    // }


}

class DptRepository(
    private val db: Database,
    private val collegeRepo: CollegeRepostitory
) {
    suspend fun getIdOrNull(dpt: Dpt): Int? = suspendTx(db, Dispatchers.IO) {
        val collegeId = collegeRepo.getIdOrNull(dpt.college) ?: return@suspendTx null

        val result = DptTbl.select(DptTbl.id)
            .where { (DptTbl.code eq dpt.code) and (DptTbl.name eq dpt.name) and (DptTbl.collegeId eq collegeId) }

        return@suspendTx result.firstOrNull()?.let { it[DptTbl.id].value }
    }

    suspend fun insertIfNotExistsCascade(dpt: Dpt): Int = suspendTx(db, Dispatchers.IO) {
        val collegeId: Int = collegeRepo.insertIfNotExistsCascade(dpt.college)

        val dptId = DptTbl.insertIgnoreAndGetId {
            it[DptTbl.code] = dpt.code
            it[DptTbl.name] = dpt.name
            it[DptTbl.collegeId] = collegeId
        }?.value

        return@suspendTx dptId ?: getIdOrNull(dpt)!!
    }

    suspend fun insert(dpt: Dpt): Int = suspendTx(db, Dispatchers.IO) {
        val collegeId = collegeRepo.getIdOrNull(dpt.college)!!

        DptTbl.insertAndGetId {
            it[DptTbl.code] = dpt.code
            it[DptTbl.name] = dpt.name
            it[DptTbl.collegeId] = collegeId
        }.value
    }
}

class CollegeRepostitory(
    private val db: Database,
    private val termRepository: TermRepository,
) {
    suspend fun getIdOrNull(college: College): Int? = suspendTx(db, Dispatchers.IO) {
        val termId = termRepository.getIdOrNull(college.term) ?: return@suspendTx null
        val result = CollegeTbl.select(CollegeTbl.id)
            .where { (CollegeTbl.code eq college.code) and (CollegeTbl.name eq college.name) and (CollegeTbl.termId eq termId) }
            .limit(1)

        return@suspendTx result.singleOrNull()?.let { it[CollegeTbl.id].value }
    }

    suspend fun insertIfNotExistsCascade(college: College): Int = suspendTx(db, Dispatchers.IO) {
        val termId: Int = termRepository.insertIfNotExists(college.term)

        val collegeid: Int? = CollegeTbl.insertIgnoreAndGetId {
            it[CollegeTbl.code] = college.code
            it[CollegeTbl.name] = college.name
            it[CollegeTbl.termId] = termId
        }?.value

        return@suspendTx collegeid ?: getIdOrNull(college)!!
    }

    suspend fun insert(college: College): Int = suspendTx(db, Dispatchers.IO) {
        val termId = termRepository.getIdOrNull(college.term)!!
        CollegeTbl.insertAndGetId {
            it[CollegeTbl.code] = college.code
            it[CollegeTbl.name] = college.name
            it[CollegeTbl.termId] = termId
        }.value
    }
}


class TermRepository(
    private val db: Database
) {
    suspend fun getIdOrNull(term: Term): Int? = suspendTx(db, Dispatchers.IO) {
        val query = TermTbl.select(TermTbl.id)
            .where { TermTbl.semester.eq(term.semester) and TermTbl.year.eq(term.year) }
            .limit(1)

        return@suspendTx query.singleOrNull()?.let { it[TermTbl.id].value }
    }

    suspend fun insertIfNotExists(term: Term): Int = suspendTx(db, Dispatchers.IO) {
        val termId: Int? = TermTbl.insertIgnoreAndGetId {
            it[TermTbl.semester] = term.semester
            it[TermTbl.year] = term.year
        }?.value
        return@suspendTx termId ?: getIdOrNull(term)!!
    }

    suspend fun insert(term: Term): Int = suspendTx(db, Dispatchers.IO) {
        TermTbl.insertAndGetId {
            it[TermTbl.semester] = term.semester
            it[TermTbl.year] = term.year
        }.value
    }
}