package io.gitp.ysfl.client

import io.gitp.ysfl.client.payload.DptGroupPayloadVo
import io.gitp.ysfl.client.payload.DptPayloadVo
import io.gitp.ysfl.client.payload.LecturePayloadVo
import io.gitp.ysfl.client.response.DptGroupResp
import io.gitp.ysfl.client.response.DptResp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.time.Year
import java.util.concurrent.CompletableFuture


fun demo1() {
    val year = Year.of(2025)
    val semester = Semester.FIRST
    val dptGroupPayload = DptGroupPayloadVo(year, semester)

    YonseiClients.dptGroupClient
        .requestAndMap(dptGroupPayload.build()).get()
        .associate { dptGroup ->
            val dptPayload = DptPayloadVo(dptGroup.dptGroupId, year, semester)
            val dptList = YonseiClients.dptClient.requestAndMap(dptPayload.build())

            Pair(dptGroup, dptList)
        }
        .onEach { (dptGroup: DptGroupResp, dptList: CompletableFuture<List<DptResp>>) ->
            println(dptGroup)
            dptList.get().forEach { println("\t${it}") }
        }

}

fun demo2() {
    val lectureClient: YonseiClient<DptResp> = YonseiClient.of<DptResp>(
        "https://underwood1.yonsei.ac.kr/sch/sles/SlessyCtr/findAtnlcHandbList.do",
        postJsonRefiner = { jsonElement: JsonElement ->
            jsonElement.jsonObject["dsSles251"] ?: throw IllegalStateException("exception while post json refining")
        }
    )

    val payload = LecturePayloadVo(Year.of(2025), Semester.FIRST, "s1102", "0201")
    val respBody: String = lectureClient.request(payload.build()).get().body()
    Json.parseToJsonElement(respBody)
        .let { it.jsonObject["dsSles251"]!! }
        .jsonArray
        .onEach {
            println(it.jsonObject)
        }
}

fun demo3() {
    // val lectureClient: YonseiClient<JsonObject> = YonseiClients./*l*/ectureClientTmp
    //
    // val payload = LecturePayloadVo(Year.of(2025), Semester.FIRST, "s1102", "0201")
    // lectureClient.requestAndMap(payload.build()).get().forEach { println(it) }
}

fun main() {
    // demo1()
    demo2()
}
