package io.gitp.ylfs.parse_load_job

import io.gitp.ylfs.entity.model.College
import io.gitp.ylfs.entity.model.Dpt
import io.gitp.ylfs.entity.model.Lecture
import io.gitp.ylfs.entity.model.Term
import io.gitp.ylfs.parse_load_job.tables.CollegeTbl
import io.gitp.ylfs.parse_load_job.tables.DptTbl
import io.gitp.ylfs.parse_load_job.tables.LectureTbl
import io.gitp.ylfs.parse_load_job.tables.TermTbl
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.withSuspendTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.vendors.ForUpdateOption

private suspend fun Transaction.getTermIdOrNull(term: Term): Int? = withSuspendTransaction {
    val ifExistsQuery =
        TermTbl.select(TermTbl.id)
            .forUpdate(ForUpdateOption.ForUpdate)
            .where((TermTbl.year eq term.year) and (TermTbl.semester eq term.semester))
            .limit(1)

    return@withSuspendTransaction ifExistsQuery.singleOrNull()?.let {
        return@withSuspendTransaction it[TermTbl.id].value
    }
}

private suspend fun Transaction.getCollegeIdOrNull(college: College): Int? =
    withSuspendTransaction {
        val ifExistsQuery =
            (CollegeTbl innerJoin TermTbl)
                .select(CollegeTbl.id, TermTbl.id)
                .forUpdate(ForUpdateOption.ForUpdate)
                .where(
                    (TermTbl.year eq college.term.year) and
                            (TermTbl.semester eq college.term.semester) and
                            (CollegeTbl.code eq college.code)
                )
                .limit(1)

        return@withSuspendTransaction ifExistsQuery.singleOrNull()?.let {
            it[CollegeTbl.id].value
        }
    }

private suspend fun Transaction.getDptIdOrNull(dpt: Dpt): Int? = withSuspendTransaction {
    val ifExistsQuery =
        (DptTbl innerJoin CollegeTbl)
            .select(DptTbl.id)
            .forUpdate(ForUpdateOption.ForUpdate)
            .where((CollegeTbl.code eq dpt.college.code) and (DptTbl.code eq dpt.code))
            .limit(1)

    return@withSuspendTransaction ifExistsQuery.singleOrNull()?.let { it[DptTbl.id].value }
}

private suspend fun Transaction.getLectureIdOrNull(lecture: Lecture): Int? =
    withSuspendTransaction {
        val ifExistsQuery =
            (LectureTbl innerJoin TermTbl)
                .select(LectureTbl.id)
                .forUpdate(ForUpdateOption.ForUpdate)
                .where(
                    (LectureTbl.mainCode eq lecture.mainCode) and
                            (LectureTbl.classCode eq lecture.classCode) and
                            (TermTbl.semester eq lecture.term.semester) and
                            (TermTbl.year eq lecture.term.year)
                )
                .limit(1)

        return@withSuspendTransaction ifExistsQuery.singleOrNull()?.let {
            it[LectureTbl.id].value
        }
    }

private suspend fun Transaction.insertTermIFNotExists(term: Term): Int = withSuspendTransaction {
    getTermIdOrNull(term)?.let {
        return@withSuspendTransaction it
    }

    val termId =
        TermTbl.insertAndGetId {
            it[TermTbl.semester] = term.semester
            it[TermTbl.year] = term.year
        }
            .value
    return@withSuspendTransaction termId
}

private suspend fun Transaction.insertCollegeIfNotExists(college: College): Int =
    withSuspendTransaction {
        getCollegeIdOrNull(college)?.let {
            return@withSuspendTransaction it
        }
        val termId = getTermIdOrNull(college.term)!!

        val collegeId: Int =
            CollegeTbl.insertAndGetId {
                it[CollegeTbl.code] = college.code
                it[CollegeTbl.name] = college.name
                it[CollegeTbl.termId] = termId
            }
                .value

        return@withSuspendTransaction collegeId
    }

private suspend fun Transaction.insertDptIfNotExists(dpt: Dpt): Int = withSuspendTransaction {
    getDptIdOrNull(dpt)?.let {
        return@withSuspendTransaction it
    }

    val collegeId = getCollegeIdOrNull(dpt.college)!!

    val dptId: Int =
        DptTbl.insertAndGetId {
            it[DptTbl.code] = dpt.code
            it[DptTbl.name] = dpt.name
            it[DptTbl.collegeId] = collegeId
        }
            .value

    return@withSuspendTransaction collegeId
}

private suspend fun Transaction.insertLectureIfNotExists(lecture: Lecture): Int =
    withSuspendTransaction {
        getLectureIdOrNull(lecture)?.let {
            return@withSuspendTransaction it
        }

        val termId = getTermIdOrNull(lecture.term)!!

        val id =
            LectureTbl.insertAndGetId {
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
            }
                .value

        return@withSuspendTransaction id
    }

suspend fun load(db: Database, lecture: Lecture) =
    newSuspendedTransaction(db = db) {
        // load term
        insertTermIFNotExists(lecture.term)
        // load college and dpt
        lecture.dptAndLectureType.keys.map { dpt ->
            insertCollegeIfNotExists(dpt.college)
            insertDptIfNotExists(dpt)
        }
        // load lecture && dpt
    }

fun loadV2(db: Database, lectures: List<Lecture>) =
    runBlocking(Dispatchers.IO) {
        val distinctTerms: List<Term> = lectures.map { it.term }.distinct()
        val distinctDpts: List<Dpt> = lectures.flatMap { it.dptAndLectureType.keys }
        val distinctColleges: List<College> = distinctDpts.map { it.college }.distinct()

        distinctTerms
            .map { term ->
                launch {
                    newSuspendedTransaction(db = db) {
                        addLogger()
                        insertTermIFNotExists(term)
                    }
                }
            }
            .joinAll()
        distinctColleges
            .map { college ->
                async {
                    newSuspendedTransaction(db = db) {
                        val id = getCollegeIdOrNull(college)
                        println("id of ${college.name} ${college.code} = $id")
                        if (id == null) college else null
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
            .map { college ->
                launch {
                    newSuspendedTransaction(db = db) {
                        val termId = getTermIdOrNull(college.term)!!

                        val collegeId: Int =
                            CollegeTbl.insertAndGetId {
                                it[CollegeTbl.code] = college.code
                                it[CollegeTbl.name] = college.name
                                it[CollegeTbl.termId] = termId
                            }
                                .value
                    }
                }
            }
            .joinAll()

        distinctDpts
            .map { dpt ->
                async {
                    newSuspendedTransaction(db = db) {
                        val id = getDptIdOrNull(dpt)
                        commit()
                        if (id == null) dpt else null
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
            .map { dpt ->
                async {
                    newSuspendedTransaction(db = db) {
                        val collegeId = getCollegeIdOrNull(dpt.college)!!

                        val dptId: Int =
                            DptTbl.insertAndGetId {
                                it[DptTbl.code] = dpt.code
                                it[DptTbl.name] = dpt.name
                                it[DptTbl.collegeId] = collegeId
                            }
                                .value
                        commit()
                    }
                }
            }
            .awaitAll()

        // lectures.map { lecture ->
        //     launch {
        //         newSuspendedTransaction(db = db, transactionIsolation =
        // Connection.TRANSACTION_READ_UNCOMMITTED) {
        //             insertLectureIfNotExists(lecture)
        //         }
        //
        //     }
        // }.joinAll()
        //
        // // load term
        // insertTermIFNotExists(lecture.term)
        // // load college and dpt
        // lecture
        //     .dptAndLectureType
        //     .keys
        //     .map { dpt ->
        //         insertCollegeIfNotExists(dpt.college)
        //         // insertDptIfNotExists(dpt)
        //     }
        // // load lecture && dpt
    }

fun main() {
    val crawlDB: Database =
        Database.connect(
            url = "jdbc:mysql://43.202.5.149:3306/crawl",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "root",
            password = "root_pass"
        )
    // val connPoolConfig = HikariConfig().apply {
    //     jdbcUrl = "jdbc:mysql://43.202.5.149:3306/ylfs"
    //     driverClassName = "com.mysql.cj.jdbc.Driver"
    //     username = "root"
    //     password = "root_pass"
    //     maximumPoolSize = 40
    //     isReadOnly = false
    // }
    //
    // val db = Database.connect(datasource = HikariDataSource(connPoolConfig))
    val db: Database =
        Database.connect(
            url = "jdbc:mysql://43.202.5.149:3306/ylfs",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "root",
            password = "root_pass"
        )
    val crawledLectureRepo = CrawledLectureViewRepository(crawlDB)
    val lectures = crawledLectureRepo.findAll()

    transaction(db) {
        exec("SET FOREIGN_KEY_CHECKS = 0;")
        exec("truncate term;")
        exec("truncate college;")
        exec("truncate dpt;")
        exec("truncate dpt_lecture;")
        exec("truncate lecture;")
        exec("SET FOREIGN_KEY_CHECKS = 1;")
    }

    // val termRepo = TermRepository(db)
    // val collegeRepo = CollegeRepostitory(db, termRepo)
    // val dptRepo = DptRepository(db, collegeRepo)
    // val lectureRepo = LectureRepository(db, dptRepo, termRepo)

    runBlocking {
        lectures.slice(0..lectures.size - 1 step 10).chunked(20).map { lectureChunkcs ->
            lectureChunkcs.map { launch(Dispatchers.IO) { load(db, it) } }.joinAll()
            // delay(1000)
        }
        // .joinAll()

    }

    // lectures
    //     .slice(200..<400)
    //     .chunked(40)
    //     .map { chunks ->
    //         loadV2(db, chunks)
    //         println("looped")
    //     }
}
