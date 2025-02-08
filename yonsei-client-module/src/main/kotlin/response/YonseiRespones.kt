package io.gitp.ysfl.client.response

import kotlinx.serialization.json.*

private val jsonMapper = Json { ignoreUnknownKeys = true }
private inline fun <reified T> deserializeResp(respBody: String, postJsonRefiner: (JsonElement) -> JsonElement): List<T> =
    respBody
        .let { resp -> jsonMapper.parseToJsonElement(resp) }
        .let { jsonElement: JsonElement -> postJsonRefiner(jsonElement) }
        .let { jsonElement: JsonElement -> jsonMapper.decodeFromJsonElement<List<T>>(jsonElement) }

interface YonseiResponse

data class DptGroupResponse(
    val responseBody: String,
    val dptGroupList: List<DptGroup>,
) : YonseiResponse {
    companion object {
        private val postJsonRefiner: (JsonElement) -> JsonElement = { json: JsonElement ->
            json.jsonObject["dsUnivCd"] ?: throw IllegalStateException("DptResponse returned null body")
        }

        operator fun invoke(responseBody: String): DptGroupResponse =
            DptGroupResponse(responseBody, deserializeResp<DptGroup>(responseBody, postJsonRefiner))
    }
}

data class DptResponse(
    val responseBody: String,
    val dptList: List<Dpt>,
) : YonseiResponse {
    companion object {
        private val postJsonRefiner: (JsonElement) -> JsonElement = { json: JsonElement ->
            json.jsonObject["dsFaclyCd"] ?: throw IllegalStateException("DptResponse returned null body")
        }

        operator fun invoke(responseBody: String): DptResponse = DptResponse(responseBody, deserializeResp<Dpt>(responseBody, postJsonRefiner))
    }
}

data class LectureResponse(
    val responseBody: String,
    val lectureList: List<Lecture>,
) : YonseiResponse {
    companion object {
        private val postJsonRefiner: (JsonElement) -> JsonElement = { json: JsonElement ->
            json.jsonObject["dsSles251"] ?: throw IllegalStateException("DptResponse returned null body")
        }

        operator fun invoke(responseBody: String): LectureResponse =
            LectureResponse(responseBody, deserializeResp<Lecture>(responseBody, postJsonRefiner))
    }
}

// TODO
data class MileageResponse(
    val responseBody: String,
    val jsonObject: JsonObject,
) : YonseiResponse {
    companion object {
        private val postJsonRefiner: (JsonElement) -> JsonElement = { json: JsonElement ->
            json.jsonObject["dsSles440"] ?: throw IllegalStateException("DptResponse returned null body")
        }

        operator fun invoke(responseBody: String): MileageResponse =
            MileageResponse(responseBody, jsonMapper.parseToJsonElement(responseBody).jsonObject)
    }
}