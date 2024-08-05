package com.microsoft.notes.ui.extensions

fun CharSequence.parseSearchQuery(): List<String> = split(Regex("\\s+"))
