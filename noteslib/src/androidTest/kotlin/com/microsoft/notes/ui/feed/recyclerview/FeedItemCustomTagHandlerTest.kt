package com.microsoft.notes.ui.feed.recyclerview

import android.content.Context
import android.text.Html
import android.text.Spanned
import androidx.test.InstrumentationRegistry
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.TestNotesLibraryConfiguration
import com.microsoft.notes.richtext.render.NotesBulletSpan
import com.microsoft.notes.richtext.render.TextLeadingMarginSpan
import com.microsoft.notes.ui.feed.recyclerview.feeditem.FeedItemCustomTagHandler
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class FeedItemCustomTagHandlerTest {

    companion object {
        const val TEXT = "this is the test string."

        lateinit var context: Context
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()

        val notesLibraryConfiguration = TestNotesLibraryConfiguration.Builder.build(context, null)
        NotesLibrary.init(notesLibraryConfiguration)
    }

    private fun getSpannedFromHtml(html: String): Spanned {
        return Html.fromHtml(
            FeedItemCustomTagHandler.prepareHtmlForListTagHandler(html),
            Html.FROM_HTML_MODE_COMPACT,
            null,
            FeedItemCustomTagHandler(context, 16)
        )
    }

    @Test
    fun should_create_spanned_with_bullet_span() {
        // basic unordered list test
        val html = "<ul>" +
            "<li> $TEXT </li>" +
            "<li> $TEXT </li>" +
            "<ul>" +
            "<li> $TEXT </li>" +
            "</ul>" +
            "<li> $TEXT </li>" +
            "</ul>"

        val result = getSpannedFromHtml(html)
        val resultSpanList = result.getSpans(0, result.length, Object::class.java)

        Assert.assertThat(resultSpanList.size, iz(4))

        for (span in resultSpanList) {
            Assert.assertThat(span, instanceOf(NotesBulletSpan::class.java))
        }

        Assert.assertThat((resultSpanList[0] as NotesBulletSpan).getIndentationLevel(), iz(0))
        Assert.assertThat((resultSpanList[1] as NotesBulletSpan).getIndentationLevel(), iz(0))
        Assert.assertThat((resultSpanList[2] as NotesBulletSpan).getIndentationLevel(), iz(1))
        Assert.assertThat((resultSpanList[3] as NotesBulletSpan).getIndentationLevel(), iz(0))
    }

    @Test
    fun should_create_spanned_with_text_leading_span() {
        // basic ordered list test
        val html = "<ol>" +
            "<li> $TEXT </li>" +
            "<li> $TEXT </li>" +
            "<ol>" +
            "<li> $TEXT </li>" +
            "</ol>" +
            "<li> $TEXT </li>" +
            "</ol>"

        val result = getSpannedFromHtml(html)
        val resultSpanList = result.getSpans(0, result.length, Object::class.java)

        Assert.assertThat(resultSpanList.size, iz(4))

        for (span in resultSpanList) {
            Assert.assertThat(span, instanceOf(TextLeadingMarginSpan::class.java))
        }

        Assert.assertThat((resultSpanList[0] as TextLeadingMarginSpan).getIndentationLevel(), iz(0))
        Assert.assertThat((resultSpanList[1] as TextLeadingMarginSpan).getIndentationLevel(), iz(0))
        Assert.assertThat((resultSpanList[2] as TextLeadingMarginSpan).getIndentationLevel(), iz(1))
        Assert.assertThat((resultSpanList[3] as TextLeadingMarginSpan).getIndentationLevel(), iz(0))
    }

    @Test
    fun should_create_appropriate_spans_for_a_mix_of_ordered_and_unoredered_list() {
        val html = "<ol>" +
            "<li> $TEXT </li>" +
            "<li> $TEXT </li>" +
            "<ul>" +
            "<li> $TEXT </li>" +
            "</ul>" +
            "<li> $TEXT </li>" +
            "</ol>"

        val result = getSpannedFromHtml(html)
        val resultSpanList = result.getSpans(0, result.length, Object::class.java)

        Assert.assertThat(resultSpanList.size, iz(4))

        for (i in resultSpanList.indices) {
            val span = resultSpanList[i]
            if (i == 2) {
                Assert.assertThat(span, instanceOf(NotesBulletSpan::class.java))
            } else {
                Assert.assertThat(span, instanceOf(TextLeadingMarginSpan::class.java))
            }
        }

        Assert.assertThat((resultSpanList[0] as TextLeadingMarginSpan).getIndentationLevel(), iz(0))
        Assert.assertThat((resultSpanList[1] as TextLeadingMarginSpan).getIndentationLevel(), iz(0))
        Assert.assertThat((resultSpanList[3] as TextLeadingMarginSpan).getIndentationLevel(), iz(0))

        Assert.assertThat((resultSpanList[2] as NotesBulletSpan).getIndentationLevel(), iz(1))
    }
}
