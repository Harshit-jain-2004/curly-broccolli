package com.microsoft.notes.noteslib.extensions

/** Http in general allows 'ISO-8859-1 charset [ISO-8859-1]' which contains Non-Ascii characters as well
 * OkHttp3 library allows only Printable Ascii characters to be passed as http header value
 * There do exist api 'addUnsafeNonAscii' which allows passing non Ascii characters as headers but
 * the request may fail if string contains non-ISO-8859-1 characters
 * So, to be safe, just stripping off all non-Ascii non printable character as desired by standard
 * okHttp3 header apis
 * */
fun sanitizeForUseAsHttpHeader(str: String): String = str.removeNonAsciiNonPrintableCharacters()

fun String.removeNonAsciiNonPrintableCharacters(): String =
    this.filter { ch -> (ch == '\t' || ch in '\u0020'..'\u007e') }
