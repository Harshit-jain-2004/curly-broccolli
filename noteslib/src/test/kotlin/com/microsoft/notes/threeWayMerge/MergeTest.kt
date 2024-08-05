package com.microsoft.notes.threeWayMerge

import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.richtext.scheme.SpanStyle
import com.microsoft.notes.threeWayMerge.merge.Selection
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.nio.file.Paths
import org.hamcrest.CoreMatchers.`is` as iz

data class RawMergeExample(
    val description: String,
    val base: String,
    val primary: String,
    val secondary: String,
    val result: String
)

data class MergeExample(
    val description: String,
    val base: Note,
    val primary: Note,
    val secondary: Note,
    val result: Note
)

class MergeTest {
    // for androidstudio
    // val TEST_FILE_PATH =
    // "notes-android-sdk/three-way-merge/src/test/kotlin/com/microsoft/notes/threeWayMerge/MergeTest.txt"

    // for terminal
    val TEST_FILE_PATH = "src/test/kotlin/com/microsoft/notes/threeWayMerge/MergeTest.txt"

    @Test
    fun should_handle_merges() {
        val rawExamples = collectRawExamples(
            Paths.get(TEST_FILE_PATH).toAbsolutePath().toFile().readLines()
        )
        val examples = parseRawExamples(rawExamples)
        for (example in examples) {
            assertThat(
                threeWayMerge(
                    example.base, example.primary,
                    example.secondary
                ),
                iz(example.result)
            )
        }
    }
}

fun collectRawExamples(rawLines: List<String>): List<RawMergeExample> {
    fun parseLine(line: String, prefix: String, lineDescription: String): String {
        if (!line.startsWith(prefix)) {
            throw Exception("Expected to find $lineDescription in '$line'")
        }

        return line.removePrefix(prefix).trim()
    }

    val rawExamples = mutableListOf<RawMergeExample>()
    var currentDescription: String? = null
    var currentBase: String? = null
    var currentPrimary: String? = null
    var currentSecondary: String? = null
    var currentResult: String? = null
    rawLines.forEach loop@{
        val line = it.trim()
        if (line.isEmpty() || line.startsWith("//") || line.startsWith("?:")) {
            return@loop
        }
        when {
            currentDescription == null -> {
                currentDescription = line
            }
            currentBase == null -> {
                currentBase = parseLine(line, "B:", "base")
            }
            currentPrimary == null -> {
                currentPrimary = parseLine(line, "1:", "primary")
            }
            currentSecondary == null -> {
                currentSecondary = parseLine(line, "2:", "secondary")
            }
            currentResult == null -> {
                currentResult = parseLine(line, "M:", "merge result")

                rawExamples.add(
                    RawMergeExample(
                        currentDescription!!, currentBase!!,
                        currentPrimary!!, currentSecondary!!, currentResult!!
                    )
                )
                currentBase = null
                currentDescription = null
                currentPrimary = null
                currentSecondary = null
                currentResult = null
            }
        }
    }
    return rawExamples
}

fun parseRawExamples(rawExamples: List<RawMergeExample>): List<MergeExample> {
    return rawExamples.map { parseRawExample(it) }
}

fun parseRawExample(rawExample: RawMergeExample): MergeExample {
    val localId = "123"
    val document = Document(blocks = listOf(Paragraph()))
    val baseDocument = document.parseExample(rawExample.base)
    val baseNote = Note(localId = localId, document = baseDocument)
    val primaryDocument = document.parseExample(rawExample.primary)
    val secondaryDocument = document.parseExample(rawExample.secondary)
    val resultDocument = document.parseExample(rawExample.result)

    return MergeExample(
        description = rawExample.description,
        base = baseNote,
        primary = baseNote.withDocument(primaryDocument),
        secondary = baseNote.withDocument(secondaryDocument),
        result = baseNote.withDocument(resultDocument)
    )
}

fun Note.withDocument(document: Document): Note {
    return this.copy(document = document)
}

sealed class Token {
    class OpenBracket : Token()
    class ClosedBracket : Token()
    class Slash : Token()
    data class Text(val text: String) : Token()
    class EndOfInput : Token()
}

fun tokenize(string: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var currentString = ""

    fun addSpecialCharacterToken(specialCharacterToken: Token) {
        if (!currentString.isEmpty()) {
            tokens.add(Token.Text(currentString))
        }
        currentString = ""
        tokens.add(specialCharacterToken)
    }

    for (char in string) {
        when (char) {
            '<' -> {
                addSpecialCharacterToken(Token.OpenBracket())
            }
            '>' -> {
                addSpecialCharacterToken(Token.ClosedBracket())
            }
            '/' -> {
                addSpecialCharacterToken(Token.Slash())
            }
            '[' -> {}
            ']' -> {}
            '\n' -> {
                throw Exception("New lines are not yet supported by tokenizer/parser")
            }
            else -> currentString += char
        }
    }
    addSpecialCharacterToken(Token.EndOfInput())
    return tokens
}

sealed class ParseChunk {
    data class PlainText(val text: String) : ParseChunk()
    data class RichText(val style: SpanStyle, val text: String) : ParseChunk()
}

fun SpanStyle.add(other: SpanStyle): SpanStyle {
    return this.copy(
        bold = this.bold || other.bold,
        italic = this.italic || other.italic,
        underline = this.underline || other.underline
    )
}

fun SpanStyle.minus(other: SpanStyle): SpanStyle {
    return this.copy(
        bold = if (other.bold) {
            false
        } else {
            this.bold
        },
        italic = if (other.italic) {
            false
        } else {
            this.italic
        },
        underline = if (other.underline) {
            false
        } else {
            this.underline
        }
    )
}

fun SpanStyle.isEmpty(): Boolean {
    return !(this.bold || this.italic || this.underline)
}

fun <T> MutableList<T>.pop(): T? {
    if (this.isEmpty()) {
        return null
    }
    return this.removeAt(0)
}

fun <T> MutableList<T>.peak(): T? {
    return this.getOrNull(0)
}

fun parseStyle(token: Token.Text): SpanStyle {
    return when (token.text) {
        "b" -> SpanStyle(bold = true)
        "i" -> SpanStyle(italic = true)
        "u" -> SpanStyle(underline = true)
        else -> throw Exception("Unrecognized style: ${token.text}")
    }
}

fun parseOpeningStyleTag(tokens: MutableList<Token>): SpanStyle {
    val styleIndicator = tokens.pop()
    val closingBracket = tokens.pop()
    if (closingBracket !is Token.ClosedBracket) {
        throw Exception("Expected closing bracket. Got: $closingBracket")
    }
    return when (styleIndicator) {
        is Token.Text -> parseStyle(
            styleIndicator
        )
        else -> throw Exception("Expected style indicator. Got: $styleIndicator")
    }
}

fun parseStyleTag(tokens: MutableList<Token>): Pair<Boolean, SpanStyle> {
    val isOpening = when (tokens.peak()) {
        is Token.Slash -> {
            tokens.pop()
            false
        }
        else -> true
    }
    return Pair(isOpening, parseOpeningStyleTag(tokens))
}

fun parse(tokens: MutableList<Token>): List<ParseChunk> {
    var currentChunk: ParseChunk = ParseChunk.PlainText(
        ""
    )
    val chunks = mutableListOf<ParseChunk>()

    while (true) {
        val nextToken = tokens.pop()
        if (nextToken is Token.EndOfInput) break

        currentChunk = when (currentChunk) {
            is ParseChunk.PlainText -> {
                when (nextToken) {
                    is Token.Text -> currentChunk.copy(text = currentChunk.text + nextToken.text)
                    is Token.OpenBracket -> {
                        chunks.add(currentChunk)
                        ParseChunk.RichText(
                            text = "",
                            style = parseOpeningStyleTag(tokens)
                        )
                    }
                    else -> throw Exception("Expecting either text or an opening bracket")
                }
            }
            is ParseChunk.RichText -> {
                when (nextToken) {
                    is Token.Text -> currentChunk.copy(text = currentChunk.text + nextToken.text)
                    is Token.OpenBracket -> {
                        val (isOpening, style) = parseStyleTag(tokens)
                        when (isOpening) {
                            true -> currentChunk.copy(style = currentChunk.style.add(style))
                            false -> {
                                val newStyle = currentChunk.style.minus(style)
                                if (newStyle.isEmpty()) {
                                    chunks.add(currentChunk)
                                    ParseChunk.PlainText("")
                                } else {
                                    if (currentChunk.text.isEmpty()) {
                                        currentChunk.copy(style = newStyle)
                                    } else {
                                        chunks.add(currentChunk)
                                        currentChunk.copy(text = "", style = newStyle)
                                    }
                                }
                            }
                        }
                    }
                    else -> throw Exception("Expected either text or a style marker. Got: $nextToken")
                }
            }
        }
    }
    chunks.add(currentChunk)
    return chunks
}

fun Document.parseExample(example: String): Document {
    val tokens = tokenize(example)
    val chunks = parse(tokens.toMutableList())
    var text = ""
    val spans = mutableListOf<Span>()
    for (chunk in chunks) {
        text += when (chunk) {
            is ParseChunk.PlainText -> chunk.text
            is ParseChunk.RichText -> {
                spans.add(
                    Span(
                        style = chunk.style, start = text.length, end = text.length + chunk.text.length,
                        flag = 0
                    )
                )
                chunk.text
            }
        }
    }

    // The tests all assume one block documents.
    val oldId = this.blocks.get(0).localId
    val startOffset = example.indexOfSafe('[')
    val endOffsetAdjustment = startOffset?.let { -1 } ?: 0
    val endOffset = example.indexOfSafe(']')?.let { it + endOffsetAdjustment }
    val selection = Selection(startBlock = 0, endBlock = 0, startOffset = startOffset ?: 0, endOffset = endOffset ?: 0)

    return this.copy(range = selection, blocks = listOf(Paragraph(localId = oldId, content = Content(text, spans.toList()))))
}

fun String.indexOfSafe(item: Char): Int? {
    val index = this.indexOf(item)
    return if (index == -1) {
        null
    } else {
        index
    }
}
