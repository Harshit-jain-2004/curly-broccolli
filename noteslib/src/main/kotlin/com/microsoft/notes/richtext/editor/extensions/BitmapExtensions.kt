package com.microsoft.notes.richtext.editor.extensions

import android.graphics.Bitmap

fun Bitmap.scaleToWidth(requiredWidth: Int): Bitmap {
    if (width == requiredWidth) {
        return this
    }
    val aspectRatio = height.toFloat() / width.toFloat()
    return Bitmap.createScaledBitmap(this, requiredWidth, (requiredWidth * aspectRatio).toInt(), false)
}
