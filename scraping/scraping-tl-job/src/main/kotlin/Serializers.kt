package io.gitp.ylfs.scraping.scraping_tl_job

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.time.Year

object YearSerializer : KSerializer<Year> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("YearSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Year) {
        encoder.encodeString(value.toString())
    }


    override fun deserialize(decoder: Decoder): Year {
        return Year.parse(decoder.decodeString())
    }
}

object BigDecimalSerailizer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("YearSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toString())
    }


    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.toString())
    }
}
