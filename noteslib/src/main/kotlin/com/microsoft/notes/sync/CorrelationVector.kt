package com.microsoft.notes.sync

import com.microsoft.notes.utils.utils.asBytes
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class CorrelationVector(encodeBase64: (ByteArray) -> String) {
    val correlationVectorBase = generateCorrelationVectorBase(encodeBase64)
    private val counter = AtomicLong()

    fun incrementAndGet(): String = "$correlationVectorBase.${counter.incrementAndGet()}"

    private fun generateCorrelationVectorBase(encodeBase64: (ByteArray) -> String): String {
        val uuidByteArray = UUID.randomUUID().asBytes()
        val base64String = encodeBase64(uuidByteArray)
        return base64String.trim('=')
    }
}
