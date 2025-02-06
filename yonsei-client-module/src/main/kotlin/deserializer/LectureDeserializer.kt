package io.gitp.ysfl.client.deserializer

import io.gitp.ysfl.client.response.LectureIdResp
import io.gitp.ysfl.client.response.LectureResp
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object LectureDeserializer : KSerializer<LectureResp> {
    private val classroomParser = LectureClassroomParser()
    private val scheduleParser = LectureScheduleParser()

    override fun deserialize(decoder: Decoder): LectureResp {
        val jsonDecoder: JsonDecoder = decoder as? JsonDecoder ?: throw IllegalStateException("only support json deserialization")
        val lectureJson: JsonObject = jsonDecoder.decodeJsonElement().jsonObject

        val lectureId = LectureIdResp(
            mainId = lectureJson["prctsCorseDvclsNo"]!!.jsonPrimitive.content,
            classDivisionId = lectureJson["estblDeprtCd"]!!.jsonPrimitive.content,
            subId = lectureJson["subjtnb"]!!.jsonPrimitive.content
        )
        val dptId = lectureJson["estblDeprtCd"]!!.jsonPrimitive.content
        val name = lectureJson["subjtNm"]!!.jsonPrimitive.content

        val schedule = scheduleParser.parseSchedule(lectureJson["lctreTimeNm"]!!.jsonPrimitive.content)        // val subIds = lectureIdStr
        val classroomList = classroomParser.parseClassroom(lectureJson["lecrmNm"]!!.jsonPrimitive.content)

        val professors = lectureJson["cgprfNm"]!!.jsonPrimitive.toString().split(",")

        return LectureResp(
            lectureId = lectureId,
            dptId = dptId,
            name = name,

            classrooms = classroomList,
            schedules = schedule,

            professors = professors
        )
    }

    // i don't have any idea
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("io.gitp.LectureIdSerializer") {
        element<Int>("lectureId")
        element<Int>("dptId")
        element<Int>("name")
        element<Int>("classrooms")
        element<Int>("schedules")
        element<Int>("professors")
    }

    override fun serialize(encoder: Encoder, value: LectureResp) = throw NotImplementedError(" serialization is not supported fuck off")
}