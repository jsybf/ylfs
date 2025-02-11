package io.gitp.ylfs.crawl.client

import io.gitp.ylfs.crawl.payload.AbstractPayload

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture

open class YonseiClient<P : AbstractPayload>(
    private val requestUrl: String,
) {
    private val client: HttpClient = HttpClient.newHttpClient()

    private fun buildHttpReq(payload: String) = HttpRequest.newBuilder()
        .uri(URI(requestUrl))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(payload))
        .build()

    fun request(payload: P): CompletableFuture<Result<String>> =
        client
            .sendAsync(buildHttpReq(payload.build()), HttpResponse.BodyHandlers.ofString())
            .thenApplyAsync { httpResp ->
                // println("requested to [${requestUrl}] payload:[${payload}]")
                val ifContentTypeJson: Boolean = httpResp.headers().allValues("content-type").any { it.contains("application/json") }
                if (httpResp.statusCode() == 200 && ifContentTypeJson) Result.success(httpResp.body())
                else Result.failure(YonseiRequestException(requestUrl, payload, httpResp.body(), httpResp.statusCode()))
            }
}

class YonseiRequestException(
    val requestUrl: String,
    val requestPayload: AbstractPayload,
    val respBody: String,
    val respStatusCode: Int
) : Exception(
    """
    request to yonsei course search server failed
    requestUrl: [${requestUrl}] 
    requestPayload: [${requestPayload}]
    requestPayloadStr(actual): [${requestPayload.build()}]
    respBody: [${respBody}]
    respStatusCode: [${respStatusCode}]
    
""".trimIndent()
)


