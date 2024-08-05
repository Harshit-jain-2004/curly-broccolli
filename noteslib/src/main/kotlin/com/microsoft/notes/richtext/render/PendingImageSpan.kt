package com.microsoft.notes.richtext.render

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import com.microsoft.notes.richtext.scheme.InlineMedia

class PendingImageSpan(val media: InlineMedia) : ImageSpan(ColorDrawable(Color.TRANSPARENT))

class PlaceholderImageSpan(val media: InlineMedia, placeholder: Drawable) : ImageSpan(placeholder)

class ImageSpanWithMedia(val media: InlineMedia, drawable: Drawable) : ImageSpan(drawable)
