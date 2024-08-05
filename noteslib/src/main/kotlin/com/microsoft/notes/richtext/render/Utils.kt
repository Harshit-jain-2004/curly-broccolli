package com.microsoft.notes.richtext.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.style.ClickableSpan
import android.view.View
import android.view.WindowManager

internal fun createDrawable(context: Context, imageUrl: String): Drawable {
    val (screenWidth, screenHeight) = calculateScreenWidthAndHeight(context)
    return createDrawable(context, Uri.parse(imageUrl), screenWidth, screenHeight)
}

internal fun createDrawable(context: Context, uri: Uri, screenWidth: Int, screenHeight: Int): Drawable {
    val inputStream = context.contentResolver.openInputStream(uri)
    val bitmap = BitmapFactory.decodeStream(inputStream)

    val scaledBitmap = getCorrectDrawableSize(
        context, bitmap, screenWidth, screenHeight,
        bitmap.width, bitmap.height
    )

    val drawable = BitmapDrawable(context.resources, scaledBitmap)

    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    inputStream?.close()

    return drawable
}

internal fun calculateScreenWidthAndHeight(context: Context): Pair<Int, Int> {
    @Suppress("UnsafeCast")
    val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    val size = Point()
    display.getSize(size)
    return Pair(size.x, size.y)
}

internal tailrec fun getCorrectDrawableSize(
    context: Context,
    bitmap: Bitmap,
    screenWidth: Int,
    screenHeight: Int,
    bitmapWidth: Int,
    bitmapHeight: Int
): Bitmap {
    val minScreenHeight = 800
    val addedWidthScreenSpace = 100
    val sizeRatioToUse = 0.8

    return if (bitmapHeight > minScreenHeight || bitmapWidth > screenWidth - addedWidthScreenSpace) {
        val bitmapNewWidth = (bitmapWidth * sizeRatioToUse).toInt()
        val bitmapNewHeight = (bitmapHeight * sizeRatioToUse).toInt()
        getCorrectDrawableSize(context, bitmap, screenWidth, screenHeight, bitmapNewWidth, bitmapNewHeight)
    } else {
        Bitmap.createScaledBitmap(bitmap, bitmapWidth, bitmapHeight, true)
    }
}

internal fun createClickableSpanToOpenMediaInFullScreen(
    mediaLocalId: String,
    fullScreenMedia: ((mediaLocalId: String) -> Unit)?
): ClickableSpan? =

    if (fullScreenMedia != null) {
        object : ClickableSpan() {
            override fun onClick(widget: View) {
                fullScreenMedia(mediaLocalId)
            }
        }
    } else {
        null
    }
