package io.gitp.ysfl.client.client

import io.gitp.ysfl.client.response.DptGroupResp
import io.gitp.ysfl.client.response.DptResp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture

interface AbstractYonseiClient<T> {
}

class YonseiClient<T>(
    private val requestUrl: String,
    val postJsonRefiner: ((JsonElement) -> JsonElement)?
) {
    private val client: HttpClient = HttpClient.newHttpClient()
    val json: Json = Json { ignoreUnknownKeys = true }

    private fun buildHttpReq(payload: String) = HttpRequest.newBuilder()
        .uri(URI(requestUrl))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(payload))
        .build()

    fun request(payload: String): CompletableFuture<HttpResponse<String>> {
        val sendAsync: CompletableFuture<HttpResponse<String>> = client.sendAsync(buildHttpReq(payload), HttpResponse.BodyHandlers.ofString())
        return sendAsync
    }

    fun requestAndMapToList(payload: String): CompletableFuture<List<T>> {
        return request(payload)
            .thenApply { httpResp: HttpResponse<String> -> mapToList(httpResp.body()) }
    }

    private fun mapToList(respStr: String): List<T> {
        val refinedJson = postJsonRefiner?.let { it(json.parseToJsonElement(respStr)) } ?: json.parseToJsonElement(respStr)
        return json.decodeFromJsonElement<List<T>>(refinedJson)
    }
    // inline fun <reified  T> requestAndMapToList(payload: String): CompletableFuture<List<T>> {
    //     return request(payload)
    //         .thenApply { httpResp: HttpResponse<String> -> mapToList(httpResp.body()) }
    // }
    //
    // inline fun <reified T> mapToList(respStr: String): List<T> {
    //     val refinedJson = postJsonRefiner?.let { it(json.parseToJsonElement(respStr)) } ?: json.parseToJsonElement(respStr)
    //     return json.decodeFromJsonElement<List<T>>(refinedJson)
    // }
}

object YonseiClients {
    val DptClient = YonseiClient<DptResp>(
        requestUrl = "https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbList.do",
        postJsonRefiner = { json: JsonElement -> json.jsonObject["dsFaclyCd"]!! }
    )
    val DptGroupClient = YonseiClient<DptGroupResp>(
        requestUrl = "https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbList.do",
        postJsonRefiner = { json: JsonElement -> json.jsonObject["dsUnivCd"]!! }
    )

}