package io.gitp.ysfl.client.response

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

private val jsonMapper = Json { ignoreUnknownKeys = true }
private inline fun <reified T> deserializeResp(respBody: String, postJsonRefiner: (JsonElement) -> JsonElement): List<T> =
    respBody
        .let { resp -> jsonMapper.parseToJsonElement(resp) }
        .let { jsonElement: JsonElement -> postJsonRefiner(jsonElement) }
        .let { jsonElement: JsonElement -> jsonMapper.decodeFromJsonElement<List<T>>(jsonElement) }

interface YonseiResponseMarker

data class DptGroupResponse(
    val responseBody: String,
    val dptGroupList: List<DptGroup>,
): YonseiResponseMarker {
    companion object {
        val postJsonRefiner: (JsonElement) -> JsonElement = { json: JsonElement ->
            json.jsonObject["dsUnivCd"] ?: throw IllegalStateException("DptResponse returned null body")
        }

        operator fun invoke(responseBody: String): DptGroupResponse =
            DptGroupResponse(responseBody, deserializeResp<DptGroup>(responseBody, postJsonRefiner))
    }
}
data class DptResponse(
    val responseBody: String,
    val dptList: List<Dpt>,
): YonseiResponseMarker {
    companion object {
        val postJsonRefiner: (JsonElement) -> JsonElement = { json: JsonElement ->
            json.jsonObject["dsFaclyCd"] ?: throw IllegalStateException("DptResponse returned null body")
        }

        operator fun invoke(responseBody: String): DptResponse = DptResponse(responseBody, deserializeResp<Dpt>(responseBody, postJsonRefiner))
    }
}

data class LectureResponse(
    val responseBody: String,
    val lectureList: List<Lecture>,
): YonseiResponseMarker {
    companion object {
        val postJsonRefiner: (JsonElement) -> JsonElement = { json: JsonElement ->
            json.jsonObject["dsSles251"] ?: throw IllegalStateException("DptResponse returned null body")
        }

        operator fun invoke(responseBody: String): LectureResponse =
            LectureResponse(responseBody, deserializeResp<Lecture>(responseBody, postJsonRefiner))
    }
}
//     val mileageClient= YonseiClient.of<JsonElement>(
//         "https://underwood1.yonsei.ac.kr/sch/sles/SlessyCtr/findMlgRankResltList.do",
//         postJsonRefiner = { jsonElement: JsonElement ->
//             jsonElement.jsonObject["dsSles440"] ?: throw IllegalStateException("exception while post json refining")
//         }
//     )
// }