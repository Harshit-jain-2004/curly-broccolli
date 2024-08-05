package com.microsoft.notes.richtext.editor.utils

import android.net.Uri
import java.io.File

fun removeFile(uri: String) {
    uri.isNotBlank().let {
        val uriObject = Uri.parse(uri)
        val file = File(uriObject.path)
        file.delete()
    }
}
