package io.gitp.ylfs.entity.model

import io.gitp.ylfs.entity.enums.Day
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object LocationSerializer : JsonContentPolymorphicSerializer<LocationUnion>(LocationUnion::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "building" in element.jsonObject -> LocationUnion.OffLine.serializer()
        "duplicateCapability" in element.jsonObject -> LocationUnion.Online.serializer()
        element.jsonObject.keys.isEmpty() -> LocationUnion.RealTimeOnline.serializer()
        else -> throw IllegalArgumentException("fuck")
    }
}

@Serializable(with = LocationSerializer::class)
sealed interface LocationUnion {

    @Serializable
    data object RealTimeOnline : LocationUnion

    @Serializable
    data class Online(val duplicateCapability: Boolean) : LocationUnion

    @Serializable
    data class OffLine(val building: String, val address: String?) : LocationUnion
}

@Serializable
data class Period(
    val day: Day,
    val period: List<Int>
)

@Serializable
data class LocAndSched(
    val period: Period,
    val location: LocationUnion,
)

data class SubclassLocSched(
    val subCode: String,
    val locAndScheds: List<LocAndSched>,
) {
    var dbId: Int? = null
}
