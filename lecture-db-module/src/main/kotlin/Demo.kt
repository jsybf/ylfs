package io.gitp.ysfl.db

import io.gitp.ysfl.db.payload.DptGroupPayloadVo
import io.gitp.ysfl.db.payload.DptPayloadVo
import io.gitp.ysfl.db.response.DptGroupResp
import io.gitp.ysfl.db.response.DptResp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.time.Year

fun requestDptGroup(year: Year, semester: Semester): HttpResponse<String> {
    val dptPayload: DptGroupPayloadVo = DptGroupPayloadVo(year, semester)

    val dptGroupUrl: String = "https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbList.do"

    val client: HttpClient = HttpClient.newHttpClient()
    val req: HttpRequest = HttpRequest.newBuilder()
        .uri(URI(dptGroupUrl))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(dptPayload.buildPayloadStr()))
        .build()

    val resp: HttpResponse<String> = client.send(req, BodyHandlers.ofString())

    return resp
}

fun requestDpt(dptId: String, year: Year, semester: Semester): HttpResponse<String> {
    val dptPayload: DptPayloadVo = DptPayloadVo(dptId, year, semester)

    val dptUrl: String = "https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbList.do"

    val client: HttpClient = HttpClient.newHttpClient()
    val req: HttpRequest = HttpRequest.newBuilder()
        .uri(URI(dptUrl))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(dptPayload.buildPayloadStr()))
        .build()

    val resp: HttpResponse<String> = client.send(req, BodyHandlers.ofString())

    return resp
}

private val json = Json { ignoreUnknownKeys = true }

fun decodeDptGroupResponse(respStr: String): List<DptGroupResp> {
    val postRefined = Json
        .parseToJsonElement(respStr)
        .jsonObject["dsUnivCd"]!!

    return json.decodeFromJsonElement<List<DptGroupResp>>(postRefined)
}

fun decodeDptResponse(respStr: String): List<DptResp> {
    val postRefined = Json
        .parseToJsonElement(respStr)
        .jsonObject["dsFaclyCd"]!!

    return json.decodeFromJsonElement<List<DptResp>>(postRefined)
}


fun main() {
    // val dptGroupResp: HttpResponse<String> = requestDptGroup(Year.of(2025), Semester.FIRST)
    // val dptResp: HttpResponse<String> = requestDpt("s1101", Year.of(2025), Semester.FIRST)
    // val dptGroupRespJson = decodeDptGroupResponse(dptGroupResp.body())
    // val dptRespJson = decodeDptResponse(dptResp.body())
    // dptGroupRespJson.forEach { println(it) }
    // dptRespJson.forEach { println(it) }
    // dptRespList.forEach { println(it) }

    val dptGroupResponse: List<DptGroupResp> = decodeDptGroupResponse(requestDptGroup(Year.of(2025), Semester.FIRST).body())
    dptGroupResponse
        .associate { dptGroupResp ->
            val dptRespStr = requestDpt(dptGroupResp.dptGroupId, Year.of(2025), Semester.FIRST)
            val dptResp: List<DptResp> = decodeDptResponse(dptRespStr.body())
            return@associate Pair(dptGroupResp, dptResp)
        }.onEach { (dptGroup, dpt) ->
            println(dptGroup)
            dpt.forEach { println("\t${it}") }
        }
}