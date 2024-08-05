package com.microsoft.notes.richtext.scheme

import java.io.Serializable
data class Content(val text: String = "", val spans: List<Span> = emptyList()) : Serializable {

    override fun toString(): String {
        val stringBldr = StringBuilder()
        stringBldr.append(this::class.java.simpleName)
            .append("text_length = ${text.length}, ")
            .append("Spans = $spans)")

        return stringBldr.toString()
    }

    fun copyAndNormalizeSpans(text: String, spans: List<Span>): Content =
        copy(text = text, spans = spans.normalize(text))
}

data class Span(val style: SpanStyle, val start: Int, val end: Int, var flag: Int) : Serializable {
    init {
        checkIndexes()
    }

    fun checkIndexes() {
        if (start < 0 || end < 0) {
            throw IndexOutOfBoundsException("Indexes can not be <0")
        }
        if (start > end) {
            throw IndexOutOfBoundsException("Start can't be greater than End")
        }
    }
}

fun List<Span>.normalize(text: String): List<Span> {
    fun validRange(span: Span, text: String): Span? {
        with(span) {
            val newStart = Math.max(0, start)
            val newEnd = Math.min(text.length, end)
            return if (newEnd >= newStart) span.copy(start = newStart, end = newEnd) else null
        }
    }

    return mapNotNull { span -> validRange(span, text) }
}

data class SpanStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false
) : Serializable {
    companion object {
        val DEFAULT = SpanStyle()
        val BOLD = SpanStyle(bold = true)
        val ITALIC = SpanStyle(italic = true)
        val BOLD_ITALIC = SpanStyle(bold = true, italic = true)
        val UNDERLINE = SpanStyle(underline = true)
        val STRIKETHROUGH = SpanStyle(strikethrough = true)
    }
}
