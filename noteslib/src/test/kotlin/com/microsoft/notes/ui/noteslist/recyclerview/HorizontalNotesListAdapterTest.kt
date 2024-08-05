package com.microsoft.notes.ui.noteslist.recyclerview

import android.app.Application
import android.content.Context
import android.os.Build
import android.text.ParcelableSpan
import android.text.SpannedString
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.RemoteData
import com.microsoft.notes.noteslib.BuildConfig
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.noteslib.TestNotesLibraryConfiguration
import com.microsoft.notes.richtext.render.parseText
import com.microsoft.notes.richtext.scheme.Block
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle
import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.richtext.scheme.SpanStyle
import com.microsoft.notes.richtext.scheme.paragraphList
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteViewHolder
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.TextNoteItemComponent
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.images.HorizontalSingleImageItemComponent
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.images.MultiImageNoteItemComponent
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Ignore("Ignore this test.We will enable it again once we move to androidX and update robolectric version")
@RunWith(RobolectricTestRunner::class)
@Config(
    constants = BuildConfig::class,
    sdk = [Build.VERSION_CODES.LOLLIPOP],
    application = Application::class
)
class HorizontalNotesListAdapterTest {
    private val LONG_TEXT = "This is a long test and we are going to have fun unit testing it"
    private val NO_ITEM_EXPANDED: Int = -1
    private val FIRST_SPAN_START = 0
    private val FIRST_SPAN_END = 2
    private val FIRST_SPAN_FLAG = 10
    private val SECOND_SPAN_START = 10
    private val SECOND_SPAN_END = 20
    private val SECOND_SPAN_FLAG = 8

    private lateinit var context: Context
    private lateinit var notesListAdapter: HorizontalNotesListAdapter

    @Before
    fun setup() {
        context = RuntimeEnvironment.application
        val notesLibraryConfiguration = TestNotesLibraryConfiguration.Builder.build(context, null)
        NotesLibrary.init(notesLibraryConfiguration)
    }

    @Test
    fun should_create_adapter_correctly() {
        val notesList = createFakeTestNotes()
        notesListAdapter = HorizontalNotesListAdapter(notesList, { Unit }, { _, _ -> Unit })
        Assert.assertThat(notesList.size, CoreMatchers.`is`(notesListAdapter.itemCount))
    }

    @Test
    fun should_items_be_the_same() {
        notesListAdapter = HorizontalNotesListAdapter(createFakeTestNotes(), { Unit }, { _, _ -> Unit })
        Assert.assertThat(notesListAdapter.getItem(0), CoreMatchers.`is`(notesListAdapter.getItem(0)))
    }

    @Test
    fun should_create_text_note_preview_correctly() {
        val recyclerView = setupRecyclerView()

        val notesList = createFakeTestNotesWithFormatting()
        notesListAdapter = HorizontalNotesListAdapter(notesList, { Unit }, { _, _ -> Unit })
        recyclerView.adapter = notesListAdapter

        val viewHolder = getViewHolder(recyclerView, NotesListAdapter.NoteItemType.TEXT)
        val noteBody = viewHolder.itemView.findViewById<TextView>(R.id.noteBody)
        Assert.assertThat(noteBody.text, CoreMatchers.`is`(CoreMatchers.instanceOf(SpannedString::class.java)))

        val content = noteBody.text as SpannedString
        val allSpanList = content.getSpans(
            FIRST_SPAN_START,
            SECOND_SPAN_END, ParcelableSpan::class.java
        )
        Assert.assertThat(allSpanList.size, CoreMatchers.`is`(2))
        Assert.assertThat(
            allSpanList[0],
            CoreMatchers.`is`(CoreMatchers.instanceOf(StyleSpan::class.java))
        ) // Test for BOLD
        Assert.assertThat(content.getSpanStart(allSpanList[0]), CoreMatchers.`is`(FIRST_SPAN_START))
        Assert.assertThat(content.getSpanEnd(allSpanList[0]), CoreMatchers.`is`(FIRST_SPAN_END))
        Assert.assertThat(allSpanList[1], CoreMatchers.`is`(CoreMatchers.instanceOf(StrikethroughSpan::class.java)))
        Assert.assertThat(content.getSpanStart(allSpanList[1]), CoreMatchers.`is`(SECOND_SPAN_START))
        Assert.assertThat(content.getSpanEnd(allSpanList[1]), CoreMatchers.`is`(SECOND_SPAN_END))
    }

    @Test
    fun should_create_single_image_note_preview_correctly() {
        val recyclerView = setupRecyclerView()

        val notesList = createFakeTestNotesWithFormatting(numImages = 1)
        notesListAdapter = HorizontalNotesListAdapter(notesList, { Unit }, { _, _ -> Unit })
        recyclerView.adapter = notesListAdapter

        val viewHolder = getViewHolder(recyclerView, NotesListAdapter.NoteItemType.SINGLE_IMAGE)
        val noteBody = viewHolder.itemView.findViewById<TextView?>(R.id.noteBody)
        Assert.assertThat(noteBody, CoreMatchers.`is`(CoreMatchers.nullValue()))
    }

    @Test
    fun should_items_be_different() {
        notesListAdapter = HorizontalNotesListAdapter(createFakeTestNotes(), { Unit }, { _, _ -> Unit })
        Assert.assertThat(
            notesListAdapter.getItem(0),
            CoreMatchers.`is`(CoreMatchers.not(notesListAdapter.getItem(1)))
        )
    }

    @Test
    fun should_create_text_viewHolder() {
        val recyclerView = setupRecyclerView()

        val notesList = createFakeTestNotes(hasParagraphs = true)
        notesListAdapter = HorizontalNotesListAdapter(notesList, { Unit }, { _, _ -> Unit })
        recyclerView.adapter = notesListAdapter

        val viewHolder = getViewHolder(recyclerView, NotesListAdapter.NoteItemType.TEXT)
        Assert.assertThat(
            viewHolder.noteView,
            CoreMatchers.`is`(
                CoreMatchers.instanceOf(
                    TextNoteItemComponent::class.java
                )
            )
        )

        val noteBody = viewHolder.itemView.findViewById<TextView>(R.id.noteBody)

        val paragraphList = notesList[0].document.paragraphList()
        with(paragraphList) {
            Assert.assertThat(paragraphList.size, CoreMatchers.`is`(1))
            val parsedContent = paragraphList.first().content.parseText()
            val expectedNoteBodyText = if (parsedContent.length > 1) {
                parsedContent.substring(0..parsedContent.length - 2)
            } else {
                parsedContent
            }
            Assert.assertThat(noteBody.text.toString(), CoreMatchers.`is`(expectedNoteBodyText))
        }
    }

    @Test
    fun should_create_single_image_without_text_viewHolder() {
        val recyclerView = setupRecyclerView()

        val notesList = createFakeTestNotes(numImages = 1)
        notesListAdapter = HorizontalNotesListAdapter(notesList, { Unit }, { _, _ -> Unit })
        recyclerView.adapter = notesListAdapter

        val viewHolder = getViewHolder(recyclerView, NotesListAdapter.NoteItemType.SINGLE_IMAGE)
        Assert.assertThat(viewHolder.noteView, CoreMatchers.`is`(CoreMatchers.instanceOf(HorizontalSingleImageItemComponent::class.java)))

        val noteImage = viewHolder.itemView.findViewById<ImageView>(R.id.noteImage)
        val noteBody = viewHolder.itemView.findViewById<TextView?>(R.id.noteBody)
        Assert.assertThat(noteImage, CoreMatchers.`is`(CoreMatchers.not(CoreMatchers.nullValue())))
        Assert.assertThat(noteBody, CoreMatchers.`is`(CoreMatchers.nullValue()))
    }

    @Test
    fun should_create_single_image_with_text_viewHolder() {
        val recyclerView = setupRecyclerView()

        val notesList = createFakeTestNotes(numImages = 1, hasParagraphs = true)
        notesListAdapter = HorizontalNotesListAdapter(notesList, { Unit }, { _, _ -> Unit })
        recyclerView.adapter = notesListAdapter

        val viewHolder = getViewHolder(recyclerView, NotesListAdapter.NoteItemType.SINGLE_IMAGE)
        Assert.assertThat(
            viewHolder.noteView,
            CoreMatchers.`is`(
                CoreMatchers.instanceOf(
                    HorizontalSingleImageItemComponent::class.java
                )
            )
        )

        val noteImage = viewHolder.itemView.findViewById<ImageView>(R.id.noteImage)
        val noteBody = viewHolder.itemView.findViewById<TextView?>(R.id.noteBody)
        Assert.assertThat(noteImage, CoreMatchers.`is`(CoreMatchers.not(CoreMatchers.nullValue())))
        Assert.assertThat(noteBody, CoreMatchers.`is`(CoreMatchers.nullValue()))
    }

    @Test
    fun should_create_multi_image_viewHolder() {
        val recyclerView = setupRecyclerView()

        val notesList = createFakeTestNotes(hasParagraphs = true, numImages = 5)
        notesListAdapter = HorizontalNotesListAdapter(notesList, { Unit }, { _, _ -> Unit })
        recyclerView.adapter = notesListAdapter

        val viewHolder = getViewHolder(recyclerView, NotesListAdapter.NoteItemType.MULTI_IMAGE)
        Assert.assertThat(
            viewHolder.noteView,
            CoreMatchers.`is`(
                CoreMatchers.instanceOf(
                    MultiImageNoteItemComponent::class.java
                )
            )
        )

        val noteBody = viewHolder.itemView.findViewById<TextView?>(R.id.noteBody)
        Assert.assertThat(noteBody, CoreMatchers.`is`(CoreMatchers.nullValue()))

        val noteImage = viewHolder.itemView.findViewById<ImageView>(R.id.noteImage1)
        val noteImage2 = viewHolder.itemView.findViewById<ImageView>(R.id.noteImage2)
        val noteImage3 = viewHolder.itemView.findViewById<ImageView>(R.id.noteImage3)
        val noteImage4 = viewHolder.itemView.findViewById<ImageView>(R.id.noteImage4)
        val imageCount = viewHolder.itemView.findViewById<TextView>(R.id.imageCount)
        Assert.assertThat(noteImage, CoreMatchers.`is`(CoreMatchers.not(CoreMatchers.nullValue())))
        Assert.assertThat(noteImage2, CoreMatchers.`is`(CoreMatchers.not(CoreMatchers.nullValue())))
        Assert.assertThat(noteImage3, CoreMatchers.`is`(CoreMatchers.not(CoreMatchers.nullValue())))
        Assert.assertThat(noteImage4, CoreMatchers.`is`(CoreMatchers.not(CoreMatchers.nullValue())))
        Assert.assertThat(imageCount, CoreMatchers.`is`(CoreMatchers.not(CoreMatchers.nullValue())))
    }

    @Test
    fun should_click_as_expected() {
        val recyclerView = setupRecyclerView()

        val notesList = createFakeTestNotes()
        var clicked = false
        notesListAdapter = HorizontalNotesListAdapter(notesList, { clicked = !clicked }, { _, _ -> Unit })
        recyclerView.adapter = notesListAdapter

        val viewHolder = getViewHolder(recyclerView)

        val noteContentLayout = viewHolder.itemView

        Assert.assertThat(clicked, CoreMatchers.`is`(false))
        noteContentLayout.performClick()
        Assert.assertThat(clicked, CoreMatchers.`is`(true))
        noteContentLayout.performClick()
        Assert.assertThat(clicked, CoreMatchers.`is`(false))
    }

    private fun setupRecyclerView(): RecyclerView {
        val recyclerView = RecyclerView(context)
        recyclerView.layoutManager = LinearLayoutManager(context)

        return recyclerView
    }

    private fun getViewHolder(
        recyclerView: RecyclerView,
        viewType: NotesListAdapter.NoteItemType = NotesListAdapter.NoteItemType.TEXT,
        positionInList: Int = 0
    ): NoteViewHolder {
        val viewHolder = notesListAdapter.onCreateViewHolder(recyclerView, viewType.id)
        notesListAdapter.onBindViewHolder(viewHolder, positionInList)
        return viewHolder
    }

    private fun createFakeTestNotes(
        numItems: Int = 50,
        hasParagraphs: Boolean = false,
        numImages: Int = 0
    ): List<Note> =
        (0 until numItems).map {
            Note(
                localId = it.toString(),
                remoteData = RemoteData(
                    it.toString() + "remote", "changeKey",
                    Note(it.toString()),
                    createdAt = it.toLong(), lastModifiedAt = it.toLong()
                ),
                document = createDocument(hasParagraphs, numImages, it.toString()),
                color = Color.PINK,
                localCreatedAt = it.toLong(),
                documentModifiedAt = it.toLong()
            )
        }

    private fun createFakeTestNotesWithFormatting(numItems: Int = 50, numImages: Int = 0): List<Note> =
        (0 until numItems).map {
            Note(
                localId = it.toString(),
                remoteData = RemoteData(
                    it.toString() + "remote", "changeKey",
                    Note(it.toString()),
                    createdAt = it.toLong(), lastModifiedAt = it.toLong()
                ),
                document = createDocumentWithFormatting(numImages, it.toString()),
                color = Color.PINK,
                localCreatedAt = it.toLong(),
                documentModifiedAt = it.toLong()
            )
        }

    private fun createDocument(hasParagraphs: Boolean, numImages: Int, itemIndex: String): Document {
        val paragraph = Paragraph(localId = "localId $itemIndex", content = Content(text = "text $itemIndex"))
        val media = InlineMedia(localId = "localId $itemIndex", localUrl = "path: $itemIndex")
        val media2 = InlineMedia(localId = "localId2 $itemIndex", localUrl = "path2: $itemIndex")

        val blocks: MutableList<Block> = mutableListOf()

        if (hasParagraphs) {
            blocks.add(paragraph)
        }
        if (numImages > 1) {
            blocks.addAll(listOf(media, media2))
        } else if (numImages == 1) {
            blocks.add(media)
        }
        return if (hasParagraphs || numImages > 0) {
            Document(blocks)
        } else {
            Document()
        }
    }

    private fun createDocumentWithFormatting(numImages: Int, itemIndex: String): Document {
        val style = ParagraphStyle(false)
        val span1 = Span(SpanStyle.BOLD, FIRST_SPAN_START, FIRST_SPAN_END, FIRST_SPAN_FLAG)
        val span2 = Span(SpanStyle.STRIKETHROUGH, SECOND_SPAN_START, SECOND_SPAN_END, SECOND_SPAN_FLAG)
        val content = Content(LONG_TEXT, listOf(span1, span2))

        val paragraph = Paragraph("localId$itemIndex", style = style, content = content)
        val media = InlineMedia(localId = "localId $itemIndex", localUrl = "path: $itemIndex")
        val media2 = InlineMedia(localId = "localId2$itemIndex", localUrl = "path2: $itemIndex")

        return if (numImages > 1) {
            Document(listOf(paragraph, media, media2))
        } else if (numImages == 1) {
            Document(listOf(paragraph, media))
        } else {
            Document(listOf(paragraph))
        }
    }
}
