package it.smartphonecombo.uecapabilityparser.io

import it.smartphonecombo.uecapabilityparser.extension.decodeHex
import it.smartphonecombo.uecapabilityparser.model.ByteArrayDeepEquals
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BwClassSerializer : KSerializer<Char> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BwClass", PrimitiveKind.CHAR)

    override fun serialize(encoder: Encoder, value: Char) = encoder.encodeChar(value)

    override fun deserialize(decoder: Decoder): Char = decoder.decodeChar().uppercaseChar()
}

object HexSerializer : KSerializer<ByteArrayDeepEquals> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("HexString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArrayDeepEquals) {
        throw NotImplementedError("Serialization not implemented.")
    }

    override fun deserialize(decoder: Decoder): ByteArrayDeepEquals {
        val string = decoder.decodeString()
        return ByteArrayDeepEquals(string.decodeHex())
    }
}

object DateTimeSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DateTimeSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Long) {
        throw NotImplementedError("Serialization not implemented.")
    }

    override fun deserialize(decoder: Decoder): Long {
        val string = decoder.decodeString()
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        return Instant.from(formatter.parse(string)).toEpochMilli()
    }
}
