package io.gitp.ylfs.crawl.crawljob

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
import java.io.StringReader
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.time.Duration
import java.time.LocalDateTime
import java.time.Year
import java.util.concurrent.atomic.AtomicInteger


private data class DptRequestId(val dptGroupId: String)

private data class CourseRequestId(val dptGroupId: String, val dptId: String)

private data class MileageRequestId(val courseId: LectureId)


private data class DptGroupResponse(
    val response: String
)

private data class DptResponse(
    val requestId: DptRequestId,
    val response: String
)

private data class CourseResponse(
    val requestId: CourseRequestId,
    val response: String
)

private data class MileageResponse(
    val requestId: MileageRequestId,
    val response: String
)

private val extractDptGroupId = Regex(""" "deptCd":"(?<dptId>\w+)" """, RegexOption.COMMENTS)
private val extractDptId = Regex(""" "deptCd":"(?<dptId>\d+)" """, RegexOption.COMMENTS)
private val extractCourseId = Regex(""" "subjtnbCorsePrcts":"([\dA-Z]{7})-(\d{2})-(\d{2})" """, RegexOption.COMMENTS)


/**
 * request to yonsei course search server and
 * persist raw json http response body to mysql
 */
internal fun crawlJob(
    mysqlUsername: String,
    mysqlPassword: String,
    mysqlHost: String,
    mysqlDatabase: String,
    year: Year,
    semester: Semester,
    requestDepth: Int
) {
    require(requestDepth in (1..4))

    val repo = RawResponseRepository(mysqlHost, mysqlDatabase, mysqlUsername, mysqlPassword, year, semester)
    repo.startJob()

    /* request DptGroup */
    val dptGroupResponse: DptGroupResponse = DptGroupClient
        .request(DptGroupPayload(year, semester))
        .get()
        .getOrNull()!!
        .let { DptGroupResponse(it) }

    repo.insertDptGroupResponse(dptGroupResponse)


    if (requestDepth == 1) {
        repo.endJob()
        return
    }


    /* request Dpt */
    val dptRequestIds: List<DptRequestId> =
        extractDptGroupId
            .findAll(dptGroupResponse.response)
            .map { matchResult: MatchResult ->
                DptRequestId(matchResult.destructured.component1())
            }
            .toList()

    val dptResponses: List<DptResponse> = dptRequestIds
        .map { dptRequestId ->
            DptClient
                .request(DptPayload(dptRequestId.dptGroupId, year, semester))
                .thenApply { responseResult: Result<String> ->
                    DptResponse(dptRequestId, responseResult.getOrNull()!!)
                }
        }
        .map { it.get() }

    repo.batchInsertDptResponses(dptResponses)

    if (requestDepth == 2) {
        repo.endJob()
        return
    }

    /* request Course */
    val courseRequestIds: List<CourseRequestId> = dptResponses
        .flatMap { (dptRequestId, response) ->
            extractDptId
                .findAll(response)
                .map { matchResult -> CourseRequestId(dptRequestId.dptGroupId, matchResult.destructured.component1()) }
        }


    val courseResponses: List<CourseResponse> = courseRequestIds
        .map { courseRequestId: CourseRequestId ->
            CourseClient
                .request(CoursePayload(courseRequestId.dptGroupId, courseRequestId.dptId, year, semester))
                .thenApply { responseResult: Result<String> ->
                    CourseResponse(courseRequestId, responseResult.getOrNull()!!)
                }
        }
        .map { it.get() }

    repo.batchInsertCourseResponses(courseResponses)

    if (requestDepth == 3) {
        repo.endJob()
        return
    }

    /* reqeust Mileages */
    val mileageRequestId: List<MileageRequestId> = courseResponses
        .flatMap { (courseRequestId, response) ->
            extractCourseId
                .findAll(response)
                .map { matchResult: MatchResult ->
                    val courseId = LectureId(
                        mainId = matchResult.destructured.component1(),
                        classDivisionId = matchResult.destructured.component2(),
                        subId = matchResult.destructured.component3()
                    )
                    MileageRequestId(courseId)
                }
        }


    val totalMileageRequest = mileageRequestId.size
    var mileageRequestCount = AtomicInteger()
    val startTime = LocalDateTime.now()
    fun duration() = Duration.between(startTime, LocalDateTime.now())

    val mileageResponses: List<MileageResponse> = mileageRequestId
        .map { mileageRequestId ->
            MileageClient
                .request(MileagePayload(mileageRequestId.courseId, year, semester))
                .thenApplyAsync { responseResult: Result<String> ->
                    val resp = MileageResponse(mileageRequestId, responseResult.getOrNull()!!)
                    repo.batchInsertMileageResponse(listOf(resp))
                    resp
                }
                .handle { result, e ->
                    println(
                        "[${mileageRequestCount.incrementAndGet()}/${totalMileageRequest}] [${duration().toSeconds()}s]"
                                + "[mainId: ${result.requestId.courseId.mainId} subId: ${result.requestId.courseId.classDivisionId}]"
                    )
                    if (e != null) throw (e)
                    result
                }
        }
        .map { it.get() }
        .toList()

    repo.batchInsertMileageResponse(mileageResponses)

    if (requestDepth == 4) {
        repo.endJob()
        return
    }

    error("request Depth should be 1~4")
}

private class RawResponseRepository(
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

    fun insertDptGroupResponse(dptGroupResp: DptGroupResponse): List<Int> = dptGroupReqInsertStmt().use { stmt ->
        requireNotNull(this.jobId)

        stmt.setCharacterStream(1, StringReader(dptGroupResp.response))
        stmt.execute()

        return stmt.generatedKeys.map { it.getInt(1) }.toList()

    }


    fun batchInsertDptResponses(dptResponses: List<DptResponse>): List<Int> = dptReqInsertStmt().use { stmt ->
        requireNotNull(this.jobId)

        dptResponses.onEach { (requestId, response) ->
            stmt.setString(1, requestId.dptGroupId)
            stmt.setCharacterStream(2, StringReader(response))
            stmt.addBatch()
        }
        stmt.executeBatch()

        return stmt.generatedKeys.map { it.getInt(1) }.toList()
    }


    fun batchInsertCourseResponses(courseResponse: List<CourseResponse>): List<Int> = courseReqInsertStmt().use { stmt ->
        requireNotNull(this.jobId)

        courseResponse.onEach { (requestId, response) ->
            stmt.setString(1, requestId.dptGroupId)
            stmt.setString(2, requestId.dptId)
            stmt.setCharacterStream(3, StringReader(response))
            stmt.addBatch()
        }

        stmt.executeBatch()

        return stmt.generatedKeys.map { it.getInt(1) }.toList()
    }

    fun batchInsertMileageResponse(mileageResponses: List<MileageResponse>): List<Int> = mileageReqInsertStmt().use { stmt ->
        println("batchInsertMileageRequest is called")
        requireNotNull(this.jobId)

        mileageResponses.onEach { (requestId, response) ->
            stmt.setString(1, requestId.courseId.mainId)
            stmt.setString(2, requestId.courseId.classDivisionId)
            stmt.setString(3, requestId.courseId.subId)
            stmt.setCharacterStream(4, StringReader(response))
            stmt.addBatch()
        }

        stmt.executeBatch()

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
