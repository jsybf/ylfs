package io.gitp.ysfl.client.deserializer

import io.gitp.ysfl.client.response.Lecture
import io.gitp.ysfl.client.response.LectureId
import io.gitp.ysfl.client.response.LocationUnion
import io.gitp.ysfl.client.response.Schedule
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

internal object LectureDeserializer : KSerializer<Lecture> {
    override fun deserialize(decoder: Decoder): Lecture {
        val jsonDecoder: JsonDecoder = decoder as? JsonDecoder ?: throw IllegalStateException("only support json deserialization")
        val lectureJson: JsonObject = jsonDecoder.decodeJsonElement().jsonObject

        val lectureId = LectureId(
            mainId = lectureJson["prctsCorseDvclsNo"]!!.jsonPrimitive.content,
            classDivisionId = lectureJson["estblDeprtCd"]!!.jsonPrimitive.content,
            subId = lectureJson["subjtnb"]!!.jsonPrimitive.content
        )
        val dptId = lectureJson["estblDeprtCd"]!!.jsonPrimitive.content
        val name = lectureJson["subjtNm"]!!.jsonPrimitive.content

        val schedules = lectureJson["lctreTimeNm"]!!.jsonPrimitive.content
        val locations = lectureJson["lecrmNm"]!!.jsonPrimitive.content
        val locationAndSchedule: Map<Schedule, LocationUnion> = LocationScheduleParser.parse(locations, schedules)

        val professors = lectureJson["cgprfNm"]!!.jsonPrimitive.toString().split(",")

        return Lecture(
            lectureId = lectureId,
            dptId = dptId,
            name = name,
            locationAndSchedule = locationAndSchedule,
            professors = professors
        )
    }

    // i don't have any idea
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("io.gitp.LectureIdSerializer") {
        TODO()
        // element<Int>("lectureId")
        // element<Int>("dptId")
        // element<Int>("name")
        // element<Int>("classrooms")
        // element<Int>("schedules")
        // element<Int>("professors")
    }

    override fun serialize(encoder: Encoder, value: Lecture) = throw NotImplementedError(" serialization is not supported fuck off")
}