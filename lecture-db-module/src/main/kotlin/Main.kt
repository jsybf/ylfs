package io.gitp.ysfl.db

import io.gitp.ysfl.client.Semester
import io.gitp.ysfl.client.YonseiClient
import io.gitp.ysfl.client.YonseiClients
import io.gitp.ysfl.client.payload.DptGroupPayloadVo
import io.gitp.ysfl.client.payload.DptPayloadVo
import io.gitp.ysfl.client.payload.LecturePayloadVo
import io.gitp.ysfl.client.response.DptGroupResp
import io.gitp.ysfl.client.response.DptResp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year
import java.util.concurrent.CompletableFuture

fun clientDemo() {
    val lectureClient: YonseiClient<JsonObject> = YonseiClients.lectureClientTmp
    val payload = LecturePayloadVo(Year.of(2025), Semester.FIRST, "s1102", "0201")
    lectureClient.requestAndMap(payload.build()).get().forEach { println(it) }
}

object DptGroupTable : Table(name = "dpt_group") {
    val dptGroupId = varchar("dpt_group_id", 30)
    val name = varchar("name", 30).uniqueIndex()
}

object DptTable : Table(name = "dpt") {
    val dptId = varchar("dpt_id", 30)
    val dptGroupId = varchar("dpt_group_id", 30)
    val name = varchar("name", 30).uniqueIndex()
}

object LectureJsonTable : Table(name = "lecture_json") {
    val dptId = varchar("dpt_id", 30).uniqueIndex()
    val json = text("json")
    val lectureJsonId = integer("lecture_json_id")
}


fun main() {
    val db: Database = Database.connect(
        url = "jdbc:mysql://43.202.5.149:3306/test_db",
        driver = "com.mysql.cj.jdbc.Driver",
        user = "root",
        password = "root_pass"
    )

    val year = Year.of(2025)
    val semester = Semester.FIRST

    val dptGroupPayload = DptGroupPayloadVo(year, semester)

    val dptGroupList: List<DptGroupResp> = YonseiClients.dptGroupClient.requestAndMap(dptGroupPayload.build()).get()

    val dptMap: List<Pair<DptGroupResp, DptResp>> = dptGroupList
        .associateWith { dptGroup ->
            val dptPayload = DptPayloadVo(dptGroup.dptGroupId, year, semester)
            val dptList = YonseiClients.dptClient.requestAndMap(dptPayload.build())
            dptList
        }
        .flatMap { dptMap ->
            val dptList: List<DptResp> = dptMap.value.get()
            dptList.map { Pair(dptMap.key, it) }
        }

    val lectureMap: List<Pair<DptResp, JsonElement>> = dptMap
        .map { (dptGroup, dpt) ->
            val payload: LecturePayloadVo = LecturePayloadVo(year, semester, dptGroup.dptGroupId, dpt.dptId)
            val lectures: CompletableFuture<List<JsonObject>> = YonseiClients.lectureClientTmp.requestAndMap(payload.build())
            Pair(dpt, lectures)
        }
        .flatMap { (dpt, lectures) ->
            val lectureList = lectures.get()
            lectureList.map { Pair(dpt, it) }
        }


    transaction(db) {
        dptGroupList.forEach { dptGroup ->
            DptGroupTable.insert {
                it[dptGroupId] = dptGroup.dptGroupId
                it[name] = dptGroup.dptGroupName
            }
        }
    }

    transaction(db) {
        dptMap
            .forEach { dptMap ->
                DptTable.insert {
                    it[dptGroupId] = dptMap.first.dptGroupId
                    it[dptId] = dptMap.second.dptId
                    it[name] = dptMap.second.dptName
                }
            }
    }
    val prettyJson = Json { prettyPrint = true }
    transaction(db) {
        lectureMap.forEach { (dpt, lectureJson) ->
            LectureJsonTable.insert {
                it[json] = prettyJson.encodeToString(lectureJson)
                it[dptId] = dpt.dptId
            }

        }
    }

    // transaction(db) {
    //     exec("select DATABASE()") { resultSet: ResultSet ->
    //         resultSet.next()
    //         resultSet.getString(1).let { println(it) }
    //
    //     }
    // }
}
