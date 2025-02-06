package io.gitp.ysfl.client.client

import io.gitp.ysfl.client.response.DptGroupResp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.util.concurrent.CompletableFuture

class DptGroupClient : AbstractYonseiClient<DptGroupResp> {
    private val requestUrl: String = "https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbList.do"

    private val client: HttpClient = HttpClient.newHttpClient()

    private val json: Json = Json { ignoreUnknownKeys = true }

    private fun buildHttpReq(payload: String) = HttpRequest.newBuilder()
        .uri(URI(requestUrl))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(payload))
        .build()

    fun request(payload: String): CompletableFuture<HttpResponse<String>> {
        val sendAsync: CompletableFuture<HttpResponse<String>> = client.sendAsync(buildHttpReq(payload), BodyHandlers.ofString())
        return sendAsync
    }

    private fun mapToList(respStr: String): List<DptGroupResp> {
        val refinedJson = json
            .parseToJsonElement(respStr)
            .jsonObject["dsUnivCd"]!!

        return json.decodeFromJsonElement<List<DptGroupResp>>(refinedJson)
    }


    fun requestAndMapToList(payload: String): CompletableFuture<List<DptGroupResp>> {
        return this.request(payload)
            .thenApply { httpResp: HttpResponse<String> -> this.mapToList(httpResp.body()) }
    }
}