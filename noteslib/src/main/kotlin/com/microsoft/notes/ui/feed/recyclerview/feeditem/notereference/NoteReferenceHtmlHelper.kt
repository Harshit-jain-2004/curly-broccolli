package com.microsoft.notes.ui.feed.recyclerview.feeditem.notereference

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.microsoft.notes.ui.feed.recyclerview.feeditem.FeedItemCustomTagHandler

/**
 * Converts the previewRichText from HTML format to Spanned
 */
@RequiresApi(Build.VERSION_CODES.N)
fun getPreviewRichTextAsSpanned(previewRichText: String, context: Context, notePreview: TextView?): Spanned {
    return Html.fromHtml(
        FeedItemCustomTagHandler.prepareHtmlForListTagHandler(previewRichText),
        Html.FROM_HTML_MODE_COMPACT,
        null, /* update when we need to handle img tags */
        FeedItemCustomTagHandler(context, notePreview?.lineHeight ?: 0)
    ).trim('\n') as Spanned
}
