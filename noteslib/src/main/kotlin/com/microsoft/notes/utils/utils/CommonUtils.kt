package com.microsoft.notes.utils.utils

import android.util.Base64
import java.nio.ByteBuffer
import java.util.UUID

const val SIZE_OF_UUID = 16

fun UUID.asBytes(): ByteArray {
    val byteBuffer = ByteBuffer.wrap(kotlin.ByteArray(16))
    byteBuffer.putLong(mostSignificantBits)
    byteBuffer.putLong(leastSignificantBits)
    return byteBuffer.array()
}

fun UUID.toBigEndianByteArray(): ByteArray =
    asBytes().toBigEndian()

private fun ByteArray.toBigEndian(): ByteArray {
    val mixedEndian: ByteArray = this
    val bigEndian = mixedEndian.copyOf(mixedEndian.size)
    bigEndian[0] = mixedEndian[3]
    bigEndian[1] = mixedEndian[2]
    bigEndian[2] = mixedEndian[1]
    bigEndian[3] = mixedEndian[0]
    bigEndian[4] = mixedEndian[5]
    bigEndian[5] = mixedEndian[4]
    bigEndian[6] = mixedEndian[7]
    bigEndian[7] = mixedEndian[6]
    return bigEndian
}

fun ByteArray.toBase64UrlEncoding(): String =
    Base64.encodeToString(this, Base64.URL_SAFE or Base64.NO_PADDING)

fun toNullabilityIdentifierString(any: Any?) = if (any == null) "Null" else "NonNull"
