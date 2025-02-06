package io.gitp.ysfl.client

import io.gitp.ysfl.client.response.DptGroupResp
import io.gitp.ysfl.client.response.DptResp
import io.gitp.ysfl.client.response.YonseiResp
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

class YonseiClient<T : YonseiResp>(
    private val requestUrl: String,
    private val kclass: KClass<T>,
    private val postJsonRefiner: ((JsonElement) -> JsonElement)
) {
    companion object {
        inline fun <reified V : YonseiResp> of(requestUrl: String, noinline postJsonRefiner: ((JsonElement) -> JsonElement)): YonseiClient<V> {
            return YonseiClient<V>(requestUrl, V::class, postJsonRefiner)
        }
    }

    private val client: HttpClient = HttpClient.newHttpClient()

    private val json: Json = Json { ignoreUnknownKeys = true }

    private fun buildHttpReq(payload: String) = HttpRequest.newBuilder()
        .uri(URI(requestUrl))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(payload))
        .build()

    fun request(payload: String): CompletableFuture<HttpResponse<String>> {
        val sendAsync: CompletableFuture<HttpResponse<String>> = client.sendAsync(buildHttpReq(payload), HttpResponse.BodyHandlers.ofString())
        return sendAsync
    }

    @OptIn(InternalSerializationApi::class)
    fun requestAndMap(payload: String): CompletableFuture<List<T>> {
        return request(payload)
            .thenApply { httpResp: HttpResponse<String> ->
                val refinedJson = httpResp.body()
                    .let { respStr: String -> json.parseToJsonElement(respStr) }
                    .let { jsonElement: JsonElement -> postJsonRefiner(jsonElement) }
                json.decodeFromJsonElement(ListSerializer(kclass.serializer()), refinedJson)
            }
    }
}

object YonseiClients {
    val dptClient = YonseiClient.of<DptResp>(
        requestUrl = "https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbList.do",
        postJsonRefiner = { json -> json.jsonObject["dsFaclyCd"] ?: throw IllegalStateException("exception while post json refining") }
    )
    val dptGroupClient = YonseiClient.of<DptGroupResp>(
        requestUrl = "https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbList.do",
        postJsonRefiner = { json -> json.jsonObject["dsUnivCd"] ?: throw IllegalStateException("exception while post json refining") }
    )
}