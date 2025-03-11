package io.gitp.ylfs.scraping.scraping_req_job

import com.zaxxer.hikari.HikariDataSource
import io.gitp.ylfs.entity.type.Semester
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.time.Year

/**
 * save raw response json
 */
internal class CrawlJobRepository(
    mysqlHost: String,
    mysqlDatabase: String,
    mysqlUser: String,
    mysqlPassword: String,

    val year: Year,
    val semester: Semester
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val connPool = run {
        val jdbcUrl = "jdbc:mysql://$mysqlHost/$mysqlDatabase"
        logger.info("connecting to mysql jdbcUrl=[${jdbcUrl}] user=[$mysqlUser] password=[$mysqlPassword]")

        HikariDataSource()
            .apply {
                setJdbcUrl(jdbcUrl)
                username = mysqlUser
                password = mysqlPassword
            }
        // DriverManager.getConnection(jdbcUrl, mysqlUser, mysqlPassword)
    }
    private var jobId: Int? = null

    fun startJob() = connPool.connection.use { conn ->
        conn.buildSql(
            """
            INSERT INTO crawl_job(year, semester)
            VALUES (?, ?);
    """
        ).use { stmt ->
            require(this.jobId == null)

            stmt.setString(1, year.toString())
            stmt.setString(2, semester.name)
            stmt.execute()

            val jobId = stmt.getIds().first()
            this.jobId = jobId


            logger.info("started crawl job")
        }
    }


    fun endJob() = connPool.connection.use { conn ->
        conn.buildSql(
            """
            UPDATE crawl_job
            SET end_datetime = CURRENT_TIMESTAMP
            WHERE crawl_job_id = ${jobId!!}
        """
        ).use { stmt ->
            requireNotNull(this.jobId)

            stmt.execute()
            this.jobId = null
            this.connPool.close()

            logger.info("crawl job end. closed jdbc connection")
        }
    }

    fun insertCollegeResp(collegeResp: CollegeResp): Int = connPool.connection.use { conn ->
        conn.buildSql(
            """
            INSERT INTO college_resp(crawl_job_id, http_resp_body)
            VALUE (${jobId!!}, ?)
        """
        ).use { stmt ->
            requireNotNull(this.jobId)
            stmt.setString(1, collegeResp.resp)
            stmt.execute()
            return stmt.getIds().first()
        }
    }


    fun batchInsertDptResps(dptResps: List<DptResp>): List<Int> = connPool.connection.use { conn ->
        conn.buildSql(
            """
        INSERT INTO dpt_resp (crawl_job_id, college_id, http_resp_body)
        VALUE (${jobId!!}, ?, ?)
    """
        ).use { stmt ->
            dptResps.onEach { (requestId, response) ->
                stmt.setString(1, requestId.collegeId)
                stmt.setString(2, response)
                stmt.addBatch()
            }
            stmt.executeBatch()

            return stmt.getIds()
        }

    }

    fun batchInsertLectureResps(lectureResps: List<LectureResp>): List<Int> = connPool.connection.use { conn ->
        conn.buildSql(
            """
        INSERT INTO lecture_resp (crawl_job_id, college_id, dpt_id, http_resp_body)
        VALUE ('${jobId!!}', ?, ?, ?)
    """.trimIndent()
        ).use { stmt ->
            requireNotNull(this.jobId)

            lectureResps.onEach { (requestId, response) ->
                stmt.setString(1, requestId.collegeId)
                stmt.setString(2, requestId.dptId)
                stmt.setString(3, response)
                stmt.addBatch()
            }
            stmt.executeBatch()

            return stmt.getIds()
        }
    }

    fun batchInsertMlgRankResps(mlgRankResps: List<MlgRankResp>): List<Int> = connPool.connection.use { conn ->
        conn.buildSql(
            """
                INSERT INTO mlg_rank_resp (crawl_job_id, main_id, class_id, sub_id, http_resp_body)
                VALUE ('${jobId!!}', ?, ?, ?, ?)
            """.trimIndent()
        ).use { stmt ->
            requireNotNull(this.jobId)
            mlgRankResps.onEach { (requestId, response) ->
                stmt.setString(1, requestId.lectureId.mainId)
                stmt.setString(2, requestId.lectureId.classId)
                stmt.setString(3, requestId.lectureId.subId)
                stmt.setString(4, response)
                stmt.addBatch()
            }

            stmt.executeBatch()

            return stmt.getIds()
        }
    }

    fun batchInsertMlgInfoResps(mlgRankResps: List<MlgInfoResp>): List<Int> = connPool.connection.use { conn ->
        conn.buildSql(
            """
                INSERT INTO mlg_info_resp (crawl_job_id, main_id, class_id, sub_id, http_resp_body)
                VALUE ('${jobId!!}', ?, ?, ?, ?)
            """.trimIndent()
        ).use { stmt ->
            requireNotNull(this.jobId)
            mlgRankResps.onEach { (requestId, response) ->
                stmt.setString(1, requestId.lectureId.mainId)
                stmt.setString(2, requestId.lectureId.classId)
                stmt.setString(3, requestId.lectureId.subId)
                stmt.setString(4, response)
                stmt.addBatch()
            }

            stmt.executeBatch()

            return stmt.getIds()
        }
    }

}

private fun Connection.buildSql(sql: String): PreparedStatement {
    return this.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
}

private fun Statement.getIds(idIdx: Int = 1): List<Int> {
    return this.generatedKeys.map { it.getInt(idIdx) }.toList()
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
