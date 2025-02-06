package io.gitp.ysfl.db

import io.gitp.ysfl.db.payload.DptGroupPayloadVo
import io.gitp.ysfl.db.payload.DptPayloadVo
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

fun main() {
    val dptGroupResp: HttpResponse<String> = requestDptGroup(Year.of(2024), Semester.FIRST)
    val dptResp: HttpResponse<String> = requestDpt("s1101", Year.of(2024), Semester.FIRST)

    println(dptGroupResp.body())
    println(dptResp.body())

}