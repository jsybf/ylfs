package io.gitp.ysfl.client

import io.gitp.ysfl.client.payload.AbstractPayloadVo
import io.gitp.ysfl.client.response.DptGroupResponse
import io.gitp.ysfl.client.response.DptResponse
import io.gitp.ysfl.client.response.LectureResponse
import io.gitp.ysfl.client.response.YonseiResponseMarker
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture

abstract class YonseiClient<T : YonseiResponseMarker>(
    private val requestUrl: String,
    private val mapper: ((HttpResponse<String>) -> T)
) {
    private val client: HttpClient = HttpClient.newHttpClient()

    private fun buildHttpReq(payload: String) = HttpRequest.newBuilder()
        .uri(URI(requestUrl))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(payload))
        .build()

    fun request(payload: AbstractPayloadVo): CompletableFuture<T> =
        client
            .sendAsync(buildHttpReq(payload.build()), HttpResponse.BodyHandlers.ofString())
            .thenApply { httpResp ->
                if (httpResp.statusCode() != 200) {
                    throw IllegalStateException(
                        """
                        request failed
                        - requestPayload: ${payload} 
                        - response statusCode : ${httpResp.statusCode()} 
                        - response ${httpResp}
                    """.trimIndent()
                    )
                }
                httpResp
            }
            .thenApply { mapper(it) }
}

public class DptClient : YonseiClient<DptResponse>(
    requestUrl = "https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbList.do",
    mapper = { resp -> DptResponse(resp.body()) }
)

public class DptGroupClient : YonseiClient<DptGroupResponse>(
    requestUrl = "https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbList.do",
    mapper = { resp -> DptGroupResponse(resp.body()) }
)

public class LectureClient : YonseiClient<LectureResponse>(
    requestUrl = "https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbList.do",
    mapper = { resp -> LectureResponse(resp.body()) }
)