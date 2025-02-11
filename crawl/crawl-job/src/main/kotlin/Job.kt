package io.gitp.ylfs.crawl.crawljob

import com.zaxxer.hikari.HikariConfig
import io.gitp.ylfs.crawl.client.CourseClient
import io.gitp.ylfs.crawl.client.DptClient
import io.gitp.ylfs.crawl.client.DptGroupClient
import io.gitp.ylfs.crawl.client.MileageClient
import io.gitp.ylfs.crawl.payload.CoursePayload
import io.gitp.ylfs.crawl.payload.DptGroupPayload
import io.gitp.ylfs.crawl.payload.DptPayload
import io.gitp.ylfs.crawl.payload.MileagePayload
import io.gitp.ylfs.entity.type.LectureId
import io.gitp.ylfs.entity.type.Semester
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.future.asDeferred
import java.io.StringReader
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.time.Year
import java.util.concurrent.Executor
import java.util.concurrent.Executors


private typealias PairList<T, V> = List<Pair<T, V>>
private typealias TripleList<T, V, U> = List<Triple<T, V, U>>


private typealias DptGroupRespStr = String
private typealias DptRespStr = String
private typealias CourseRespStr = String
private typealias MileageRespStr = String

private typealias DptIdStr = String
private typealias DptGroupIdStr = String

val extractDptGroupId = Regex(""" "deptCd":"(?<dptId>\w+)" """, RegexOption.COMMENTS)
val extractDptId = Regex(""" "deptCd":"(?<dptId>\d+)" """, RegexOption.COMMENTS)
val extractCourseId = Regex(""" "subjtnbCorsePrcts":"([\dA-Z]{7})-(\d{2})-(\d{2})" """, RegexOption.COMMENTS)


/**
 * request to yonsei course search server and
 * persist raw json http response body to mysql
 */
internal fun crawlJob(arg: Args) {

    val testExecutor: Executor = Executors.newFixedThreadPool(40)
    val repo = RequestRepository(arg.mysqlHost, arg.mysqlDatabase, arg.mysqlUsername, arg.mysqlPassword, arg.year, arg.semester)
    repo.startJob()

    /* request DptGroup */
    val dptGroupResp: DptGroupRespStr = DptGroupClient
        .request(DptGroupPayload(arg.year, arg.semester))
        .get()
        .onFailure { e -> println(e) }
        .getOrNull()!!
    val dptGroupIds: List<DptGroupIdStr> =
        extractDptGroupId
            .findAll(dptGroupResp)
            .map { mr: MatchResult -> mr.destructured.component1() }
            .toList()


    /* request Dpt */
    val dptResp: PairList<DptRespStr, DptGroupIdStr> = dptGroupIds
        .map { dptGroupId -> DptClient.request(DptPayload(dptGroupId, arg.year, arg.semester)) }
        .map { it.get().onFailure { e -> println(e) }.getOrNull()!! }
        .zip(dptGroupIds)
    val dptIdMap: PairList<DptGroupIdStr, DptIdStr> = dptResp
        .flatMap { (dptResp, dptGroupId) ->
            extractDptId
                .findAll(dptResp)
                .map { it.destructured.component1() }
                .map { Pair(dptGroupId, it) }
        }
    // .onEach { println(it) }


    /* request course */
    val courseResp: PairList<CourseRespStr, DptIdStr> = dptIdMap
        .map { (dptGroupId, dptId) -> CourseClient.request(CoursePayload(dptGroupId, dptId, arg.year, arg.semester)) }
        .map { it.get().onFailure { e -> println(e) }.getOrNull()!! }
        .zip(dptIdMap.map { it.second })
    val courseIdMap: PairList<DptIdStr, LectureId> = courseResp
        .flatMap { (courseResp, dptId) ->
            extractCourseId
                .findAll(courseResp)
                .map { it.destructured }
                .map { (mainid, classid, subId) -> LectureId(mainid, classid, subId) }
                .map { Pair(dptId, it) }
        }
    // .onEach { println(it) }

    /* request mileage */
    var i = 0

    runBlocking(Dispatchers.IO) {
        val payloads = courseIdMap
            .map { it.second }
            .map { MileagePayload(it, arg.year, arg.semester) }

        fun req(payload: MileagePayload): Deferred<String> = MileageClient.request(payload).thenApply { it.getOrNull()!! }.asDeferred()

        payloads
            .chunked(64)
            .zip(courseIdMap.map { it.second }.chunked(64))
            .map { (payloadChunk, courseIdChunks) ->
                    val foo = payloadChunk.map{ req(it) }.awaitAll().zip(courseIdChunks)
                    repo.batchInsertMileageRequest(foo)
            }
    }

    // courseIdMap
    //     .map { it.second }
    //     .map { MileagePayload(it, arg.year, arg.semester) }
    //     .chunked(40)
    //     .map { payloadChunk ->
    //         payloadChunk.map { payload -> MileageClient.request(payload) }
    //             .let { respFutures ->
    //                 CompletableFuture.allOf(*respFutures.toTypedArray())
    //                     .thenApplyAsync { it }
    //             }
    //
    //
    //     }
    // .map { (_, courseId) ->
    //     MileageClient
    //         .request(MileagePayload(courseId, arg.year, arg.semester))
    //         .thenApplyAsync({ resp ->
    //             println("inserting ${i++}'th")
    //             repo.batchInsertMileageRequest(listOf(Pair(resp.getOrNull()!!, courseId)))
    //         }, testExecutor)
    // }
    // val mileageResp: PairList<MileageRespStr, LectureId> = courseIdMap
    //     .map { (_, courseId) ->
    //         MileageClient
    //             .request(MileagePayload(courseId, arg.year.minusYears(1), arg.semester))
    //     }
    //     .map { it.get().onFailure { e -> println(e) }.getOrNull()!! }
    //     .zip(courseIdMap.map { it.second })
    // .onEach { println(it) }


    /* persist */

    val courseReqInfos: TripleList<DptGroupIdStr, DptIdStr, CourseRespStr> = courseResp
        .map { (courseResp, dptId) ->
            val dptGroupId = dptIdMap.find { it.second == dptId }!!.first
            Triple(dptGroupId, dptId, courseResp)
        }


    repo.insertDptGroupRequest(dptGroupResp)
    repo.batchInsertDptRequest(dptResp)
    repo.batchInsertCourseRequest(courseReqInfos)
    // repo.batchInsertMileageRequest(mileageResp)

    repo.endJob()
}

private class RequestRepository(
    mysqlHost: String,
    mysqlDatabase: String,
    mysqlUser: String,
    mysqlPassword: String,

    val year: Year,
    val semester: Semester
) {

    private val conn = run {
        val jdbcUrl = "jdbc:mysql://$mysqlHost/$mysqlDatabase"
        println("connecting to mysql jdbcUrl=[${jdbcUrl}] user=[$mysqlUser] password=[$mysqlPassword]")
        HikariConfig()
            .apply {
                this.jdbcUrl = jdbcUrl
                this.username = mysqlUser
                this.password = password
                this.maximumPoolSize = 80
            }
        DriverManager.getConnection(jdbcUrl, mysqlUser, mysqlPassword)
    }
    private var jobId: Int? = null

    private val crawlJobInsertStmt = conn.prepareStatement(
        """
        INSERT INTO crawl_job() VALUE (); 
    """, Statement.RETURN_GENERATED_KEYS
    )

    private fun crawJobEndStmt(): PreparedStatement =
        conn.prepareStatement(
            """
        UPDATE crawl_job
        SET end_datetime = CURRENT_TIMESTAMP
        WHERE crawl_job_id = ${jobId!!}
    """, Statement.RETURN_GENERATED_KEYS
        )

    private fun dptGroupReqInsertStmt(): PreparedStatement =
        conn.prepareStatement(
            """
        INSERT INTO dpt_group_request (crawl_job_id, year, semester, http_resp_body)
        VALUE (${jobId!!}, '${year}', '${semester.name}', ?)
    """, Statement.RETURN_GENERATED_KEYS
        )

    private fun dptReqInsertStmt(): PreparedStatement = conn.prepareStatement(
        """
        INSERT INTO dpt_request (crawl_job_id, year, semester, dpt_group_id, http_resp_body)
        VALUE (${jobId!!}, '${year}', '${semester.name}',?, ?)
    """, Statement.RETURN_GENERATED_KEYS
    )

    private fun courseReqInsertStmt(): PreparedStatement = conn.prepareStatement(
        """
        INSERT INTO course_request (crawl_job_id, year, semester, dpt_group_id, dpt_id, http_resp_body)
        VALUE ('${jobId!!}', '${year}', '${semester.name}', ?, ?, ?)
    """, Statement.RETURN_GENERATED_KEYS
    )

    private fun mileageReqInsertStmt(): PreparedStatement = conn.prepareStatement(
        """
        INSERT INTO mileage_request (crawl_job_id, year, semester, main_id, class_id, sub_id, http_resp_body)
        VALUE ('${jobId!!}', '${year}', '${semester.name}', ?, ?, ?, ?)
    """, Statement.RETURN_GENERATED_KEYS
    )


    fun startJob() = conn.createStatement().use { stmt ->
        require(this.jobId == null)
        stmt.execute("""INSERT INTO crawl_job() VALUES ();""", Statement.RETURN_GENERATED_KEYS)

        val jobId = stmt.generatedKeys.also { require(it.next()) }.getInt(1)
        this.jobId = jobId!!

        println("started crawl job")
    }


    fun endJob() = crawJobEndStmt().use { stmt ->
        requireNotNull(this.jobId)
        stmt.execute()

        this.jobId = null
        this.conn.close()

        println("crawl job end. close jdbc connection")
    }

    fun insertDptGroupRequest(dptGroupResp: String): List<Int> = dptGroupReqInsertStmt().use { stmt ->
        requireNotNull(this.jobId)

        stmt.setCharacterStream(1, StringReader(dptGroupResp))
        stmt.execute()

        return stmt.generatedKeys.map { it.getInt(1) }.toList()

    }


    fun batchInsertDptRequest(dptRespAndDptGroupId: PairList<DptRespStr, DptGroupIdStr>): List<Int> = dptReqInsertStmt().use { stmt ->
        requireNotNull(this.jobId)

        dptRespAndDptGroupId.onEach { (dptResp, dptGroupId) ->
            stmt.setString(1, dptGroupId)
            stmt.setCharacterStream(2, StringReader(dptResp))
            stmt.addBatch()
        }
        stmt.executeBatch()

        return stmt.generatedKeys.map { it.getInt(1) }.toList()
    }


    fun batchInsertCourseRequest(dptGroupIdAndDptIdAndCourseResp: TripleList<DptGroupIdStr, DptIdStr, CourseRespStr>): List<Int> =
        courseReqInsertStmt().use { stmt ->
            requireNotNull(this.jobId)

            dptGroupIdAndDptIdAndCourseResp.onEach { (dptGroupId, dptId, courseResp) ->
                stmt.setString(1, dptGroupId)
                stmt.setString(2, dptId)
                stmt.setCharacterStream(3, StringReader(courseResp))
                stmt.addBatch()
            }

            stmt.executeBatch()

            return stmt.generatedKeys.map { it.getInt(1) }.toList()
        }

    fun batchInsertMileageRequest(courseIdAndMileage: PairList<MileageRespStr, LectureId>): List<Int> = mileageReqInsertStmt().use { stmt ->
        println("batchInsertMileageRequest is called")
        requireNotNull(this.jobId)
        courseIdAndMileage.onEach { (mileage, courseId) ->
            stmt.setString(1, courseId.mainId)
            stmt.setString(2, courseId.classDivisionId)
            stmt.setString(3, courseId.subId)
            stmt.setCharacterStream(4, StringReader(mileage))
            stmt.addBatch()
        }
        try {
            stmt.executeBatch()
        } catch (e: Exception) {
            println("fucking excpetion ${e}")
        }

        return stmt.generatedKeys.map { it.getInt(1) }.toList()
    }

}


private fun <T> ResultSet.map(transform: (ResultSet) -> T): Iterable<T> {

    val rs = this

    val iterator = object : Iterator<T> {
        override fun hasNext(): Boolean = rs.next()

        override fun next(): T = transform(rs)
    }

    return object : Iterable<T> {
        override fun iterator(): Iterator<T> = iterator
    }
}
