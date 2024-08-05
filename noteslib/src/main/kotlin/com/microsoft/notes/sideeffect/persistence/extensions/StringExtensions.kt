package com.microsoft.notes.sideeffect.persistence.extensions

// Removes all \\s \\t \\n characters wherever they are present in the string
fun String.removeAllWhiteSpaces() = replace("[\\s\\t\\n]".toRegex(), "")
