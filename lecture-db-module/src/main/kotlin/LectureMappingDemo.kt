package io.gitp.ysfl.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.gitp.ysfl.client.PairList
import io.gitp.ysfl.client.response.Lecture
import io.gitp.ysfl.client.response.LectureResponse
import io.gitp.ysfl.db.repository.LectureRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


fun main() {


    // val db: Database = Database.connect(
    //     url = "jdbc:mysql://43.202.5.149:3306/test_db",
    //     driver = "com.mysql.cj.jdbc.Driver",
    //     user = "root",
    //     password = "root_pass"
    // )

    val connPoolConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:mysql://43.202.5.149:3306/test_db"
        driverClassName = "com.mysql.cj.jdbc.Driver"
        username = "root"
        password = "root_pass"
        maximumPoolSize = 10
        isReadOnly = false
    }

    val db = Database.connect(datasource = HikariDataSource(connPoolConfig))

    transaction {
        """
            TRUNCATE professor;
            TRUNCATE schedule;
            TRUNCATE location;
            TRUNCATE lecture_professor;
            TRUNCATE lecture_sched_loc;
            TRUNCATE lecture;
            
        """.trimIndent()
            .split("\n")
            .forEach { exec(it) }

    }

    val lectureRepo = LectureRepository(db)

    val lectures: PairList<String, Lecture> = transaction {
        LectureRequest
            .select(LectureRequest.httpRespBody, LectureRequest.dptId)
            .map {
                Pair(
                    it[LectureRequest.dptId],
                    it[LectureRequest.httpRespBody]!!
                )
            }
    }
        .flatMap { (dptId, httpRespBody) -> LectureResponse(httpRespBody).lectureList.map { lecture -> Pair(dptId, lecture) } }
    // .onEach { println(it) }

    var cnt = 0
    val total = lectures.size

    val threadPool: ExecutorService = Executors.newFixedThreadPool(10)


    lectures.forEach { (dptId, lecture) ->
        threadPool.execute {
            lectureRepo.insert(lecture, dptId)
            if (cnt % 100 == 0) println("${cnt}/${total}")
            cnt++
        }
    }

    threadPool.awaitTermination(1, TimeUnit.HOURS)
}

