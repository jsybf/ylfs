package io.gitp.yfls.scarping.job.file


import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Year

object YearSerializer : KSerializer<Year> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Year", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Year) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): Year {
        val year = decoder.decodeInt()
        return Year.of(year)
    }
}
