package io.gitp.ysfl.db.load

import io.gitp.ysfl.client.*
import io.gitp.ysfl.client.payload.DptGroupPayloadVo
import io.gitp.ysfl.client.payload.DptPayloadVo
import io.gitp.ysfl.client.payload.LecturePayloadVo
import io.gitp.ysfl.client.response.DptGroupResponse
import io.gitp.ysfl.client.response.DptResponse
import io.gitp.ysfl.client.response.LectureId
import io.gitp.ysfl.client.response.LectureResponse
import io.gitp.ysfl.db.CrawlJob
import io.gitp.ysfl.db.DptGroupRequest
import io.gitp.ysfl.db.DptRequest
import io.gitp.ysfl.db.LectureRequest
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year


fun main(args: Array<String>) {
    val dptClient: YonseiClient<DptResponse> = DptClient()
    val dptGroupClient: YonseiClient<DptGroupResponse> = DptGroupClient()
    val lectureClient: YonseiClient<LectureResponse> = LectureClient()

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
    val (dptGroupsRawResp, dptGroups) = dptGroupClient
        .request(DptGroupPayloadVo(reqYear, reqSemster))
        .get()

    transaction {
        DptGroupRequest.insert {
            it[crawlJobId] = jobId
            it[year] = reqYear.value
            it[semester] = reqSemster.name
            it[httpRespBody] = dptGroupsRawResp
        }
    }

    // dpt
    val dptGroupAndDptIdMap = mutableMapOf<String, List<String>>()
    val dptAndLecutreIdMap = mutableMapOf<String, List<LectureId>>()

    dptGroups
        .map { it.dptGroupId }
        .map { dptGroupId -> dptClient.request(DptPayloadVo(dptGroupId, reqYear, reqSemster)).thenApply { Pair(dptGroupId, it) } }
        .map { it.get() }
        .flatMap { (dptGroupId, dptResp) ->

            transaction {
                DptRequest.insert {
                    it[crawlJobId] = jobId
                    it[year] = reqYear.value
                    it[semester] = reqSemster.name
                    it[DptRequest.dptGroupId] = dptGroupId
                    it[httpRespBody] = dptResp.responseBody
                }
            }

            dptGroupAndDptIdMap[dptGroupId] = dptResp.dptList.map { it.dptId }
            dptResp.dptList
        }
        .onEach { println(it) }

    // lectures
    dptGroupAndDptIdMap
        .flatMap { (dptGroupId, dptIdList) -> dptIdList.map { Pair(dptGroupId, it) } }
        .map { (dptGroupId, dptId) ->
            Triple(
                dptGroupId,
                dptId,
                lectureClient.request(LecturePayloadVo(reqYear, reqSemster, dptGroupId, dptId))
            )
        }
        .map {(dptGroupId, dptId, lectureFuture) ->
            Triple(
                dptGroupId,
                dptId,
                lectureFuture.get()
            )
        }
        .flatMap { (dptGroupId, dptId, lectureResp) ->
            transaction {
                LectureRequest.insert {
                    it[crawlJobId] = jobId

                    it[year] = reqYear.value
                    it[semester] = reqSemster.name
                    it[LectureRequest.dptGroupId] = dptGroupId
                    it[LectureRequest.dptId] = dptId
                    it[httpRespBody] = lectureResp.responseBody
                }
            }
            dptAndLecutreIdMap[dptId] = lectureResp.lectureList.map { it.lectureId }
            lectureResp.lectureList
        }.onEach { println(it) }
}