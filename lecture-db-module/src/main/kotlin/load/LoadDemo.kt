package io.gitp.ysfl.db.load

import io.gitp.ysfl.client.Semester
import io.gitp.ysfl.client.YonseiClient
import io.gitp.ysfl.client.YonseiClients
import io.gitp.ysfl.client.payload.DptGroupPayloadVo
import io.gitp.ysfl.client.payload.DptPayloadVo
import io.gitp.ysfl.client.response.DptGroupResp
import io.gitp.ysfl.client.response.DptResp
import io.gitp.ysfl.client.response.LectureResp
import io.gitp.ysfl.db.CrawlJob
import io.gitp.ysfl.db.DptGroupRequest
import io.gitp.ysfl.db.DptRequest
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year


fun main(args: Array<String>) {
    val dptClient: YonseiClient<DptResp> = YonseiClients.dptClient
    val dptGroupClient: YonseiClient<DptGroupResp> = YonseiClients.dptGroupClient
    val lectureClient: YonseiClient<LectureResp> = YonseiClients.lectureClient

    val jsonMapper = Json { ignoreUnknownKeys = true }

    val reqYear = Year.of(2025)
    val reqSemster = Semester.FIRST

    Database.connect(
        url = "jdbc:mysql://43.202.5.149:3306/test_db",
        driver = "com.mysql.cj.jdbc.Driver",
        user = "root",
        password = "root_pass"
    )

    val jobId = transaction { CrawlJob.insert {} get CrawlJob.crawlJobId }

    // dptGroup
    val dptGroupRespBody: String = dptGroupClient
        .request(DptGroupPayloadVo(reqYear, reqSemster).build())
        .get()
        .body()
    val dptGroups: List<DptGroupResp> = dptGroupClient.mapRequestBodyToList(dptGroupRespBody)

    transaction {
        DptGroupRequest.insert {
            it[crawlJobId] = jobId
            it[year] = reqYear.value
            it[semester] = reqSemster.name
            it[httpRespBody] = dptGroupRespBody
        }
    }

    // dpt
    val dptGroupAndDptIdMap = mutableMapOf<String, List<String>>()
    val dptAndLecutreIdMap = mutableMapOf<String, List<String>>()

    dptGroups
        .map { it.dptGroupId }
        .map { dptGroupId -> dptClient.request(DptPayloadVo(dptGroupId, reqYear, reqSemster).build()).thenApply { Pair(dptGroupId, it.body()) } }
        .map { it.get() }
        .flatMap { (dptGroupId, dptResp) ->

            transaction {
                DptRequest.insert {
                    it[crawlJobId] = jobId
                    it[year] = reqYear.value
                    it[semester] = reqSemster.name
                    it[DptRequest.dptGroupId] = dptGroupId
                    it[httpRespBody] = dptResp
                }
            }

            val dpts = dptClient.mapRequestBodyToList(dptResp)
            dptGroupAndDptIdMap[dptGroupId] = dpts.map { it.dptId }
            dpts
        }.onEach { println(it) }
    //
    // dptGroupAndDptIdMap
    //     .entries
    //     .flatMap { (dptGroupId, dptIds) -> dptIds.map { dptId -> Pair(dptGroupId, dptId) } }
    //     .map { (dptGroupId, dptId) ->
    //         lectureClient.request(LecturePayloadVo(year, semester, dptGroupId, dptId).build()).thenApply { Pair(dptId, it.body()) }
    //     }
    //     .map { it.get() }
    //     .flatMap { (dptId, lectureResp) ->
    //         val lectures = lectureClient.mapRequestBodyToList(lectureResp)
    //         dptAndLecutreIdMap[dptId] = lectures.map { it.lectureId.toString() }
    //         lectures
    //     }.onEach { println(it) }


}