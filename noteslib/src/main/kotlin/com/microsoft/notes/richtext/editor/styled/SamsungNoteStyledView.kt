package com.microsoft.notes.richtext.editor.styled

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.NotesThemeOverride
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.editor.styled.ReadOnlyStyledView.ImageCallbacks
import com.microsoft.notes.richtext.editor.styled.ReadOnlyStyledView.RecordTelemetryCallback
import com.microsoft.notes.richtext.editor.styled.gallery.ITEMS_IN_A_ROW
import com.microsoft.notes.richtext.editor.styled.gallery.NoteGalleryAdapter
import com.microsoft.notes.sideeffect.sync.getSamsungMediaForPreviewImage
import com.microsoft.notes.ui.theme.ThemedFrameLayout
import kotlinx.android.synthetic.main.samsung_styled_view_layout.view.*
import kotlinx.android.synthetic.main.samsung_styled_view_layout.view.noteGalleryRecyclerView
import kotlinx.android.synthetic.main.sn_note_styled_view_layout.view.timestampText

class SamsungNoteStyledView(context: Context, attrs: AttributeSet?) :
    ThemedFrameLayout(context, attrs),
    NoteGalleryAdapter.Callback,
    ReadOnlyStyledView {

    private var sourceNote: Note? = null

    override var telemetryCallback: RecordTelemetryCallback? = null
    override var imageCallbacks: ImageCallbacks? = null
    override var ribbonCallbacks: ReadOnlyStyledView.RibbonCallbacks? = null

    private val noteGalleryAdapter = NoteGalleryAdapter()

    var themeOverride: NotesThemeOverride.SamsungNoteCanvasThemeOverride? = null

    init {
        inflate(context)
        setUpNoteGalleryRecyclerView()
    }

    override fun getEditNoteLayout(): FrameLayout = samsungNoteFrameLayout
    override fun getNoteContainerLayout(): RelativeLayout = samsungNoteContainer

    private fun getNoteTitle(): TextView = samsungNoteTitle

    private fun inflate(context: Context) {
        @Suppress("UnsafeCast")
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.samsung_styled_view_layout, this, true)
    }

    override fun onContextMenuClosed() {
        noteGalleryAdapter.menuDismissed()
    }

    private fun setTimeStamp(note: Note) {
        val noteModifiedAt = context.parseMillisToRFC1123String(note.documentModifiedAt)
        val noteCreatedAt =
            context.parseMillisToRFC1123String(note.remoteData?.createdAt ?: note.localCreatedAt)
        timestampText.text =
            context.getString(R.string.samsung_datetime_stamp, noteModifiedAt, noteCreatedAt)
    }

    private fun setUpNoteGalleryRecyclerView() {
        val layoutManager = object : GridLayoutManager(context, ITEMS_IN_A_ROW) {
            override fun isAutoMeasureEnabled(): Boolean = true
        }

        noteGalleryRecyclerView.layoutManager = layoutManager
        noteGalleryRecyclerView.isNestedScrollingEnabled = false

        noteGalleryAdapter.setCallback(this)
        noteGalleryRecyclerView.adapter = noteGalleryAdapter
    }

    override fun setNoteContent(note: Note) {
        if (!shouldRefreshNoteContent(sourceNote, note)) {
            return
        }
        sourceNote = note
        applyTheme()

        setTimeStamp(note)
        if (note.title.isNullOrEmpty()) {
            getNoteTitle().text = context.getString(R.string.samsung_no_title_placeholder)
            getNoteTitle().setTextColor(ContextCompat.getColor(context, R.color.samsung_no_title_placeholder_color))
        } else {
            getNoteTitle().text = note.title
        }

        if (shouldRenderAsHTML(note)) {
            noteGalleryRecyclerView.visibility = View.GONE
            val noteHtml = prepareNoteHTML(note)

            noteWebView.isHorizontalScrollBarEnabled = false
            noteWebView.isVerticalScrollBarEnabled = false
            noteWebView.settings.allowFileAccess = true

            // TODO: disable cookies
            noteWebView.loadDataWithBaseURL("", noteHtml, "text/html", "utf-8", "")
            noteWebView.visibility = View.VISIBLE
        } else {
            noteWebView.visibility = View.GONE
            // noteGalleryRecyclerView visibility gets set by setNoteMedia
            setNoteMedia(note)

            // The image gallery view should only take up as much space as it needs for the images
            noteGalleryRecyclerView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            sourceNote?.let { noteGalleryAdapter.setDocument(it.document) }
        }
    }

    // we receive notification to setNoteDetails even if the currently rendered note is not updated
    // so to limit these no-change ui refreshes, allowing refresh only in the following cases:
    // (sourceNote here is the currently rendered Note)
    // 1. sourceNote is null or sourceNote is different from newNote
    // 2. if both the notes are same, then update if newNote has later LMT or if the newerNote has
    //    different media
    private fun shouldRefreshNoteContent(sourceNote: Note?, newNote: Note): Boolean {
        return sourceNote == null || sourceNote.localId != newNote.localId ||
            newNote.documentModifiedAt > sourceNote.documentModifiedAt ||
            newNote.documentModifiedAt == sourceNote.documentModifiedAt &&
            newNote.media.map { Pair(it.localId, it.lastModified) } != sourceNote.media.map { Pair(it.localId, it.lastModified) }
    }

    // Note: There may be duplicate code from NotesStyledView, but all of this might go away after
    // implementing HTML rendering. So not DRYing as of now.
    // TODO 17-Dec-20 gopalsa: clean-up when implementing HTML rendering.
    private fun setNoteMedia(note: Note) {
        val media = listOfNotNull(note.media.getSamsungMediaForPreviewImage())
        if (media.isEmpty()) {
            noteGalleryRecyclerView.visibility = View.GONE
            return
        }

        val layoutManager = noteGalleryRecyclerView.layoutManager as GridLayoutManager
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int = 1
        }
        layoutManager.spanCount = 1
        noteGalleryRecyclerView.layoutManager = layoutManager

        noteGalleryRecyclerView.visibility = View.VISIBLE
        noteGalleryAdapter.setMedia(media, note.color, false)
    }

    override fun displayFullScreenImage(media: Media) {
        media.localUrl?.let {
            imageCallbacks?.openMediaInFullScreen(it, media.mimeType)
        }
    }

    fun applyTheme() {
        sourceNote?.let { setSamsungNoteColor(themeOverride) }
    }

    private fun setSamsungNoteColor(themeOverride: NotesThemeOverride.SamsungNoteCanvasThemeOverride?) {
        setSamsungNoteColor(
            contentBg = themeOverride?.contentBg ?: R.color.samsung_note_bg_for_light,
            titleColor = themeOverride?.let {
                ContextCompat.getColor(context, it.contentColor)
            } ?: ContextCompat.getColor(context, R.color.samsung_note_title_color_for_light),
            detailsColor = themeOverride?.let {
                ContextCompat.getColor(context, it.contentColor)
            } ?: ContextCompat.getColor(context, R.color.samsung_note_details_color_for_light),
            timeStampDividerColor = themeOverride?.let {
                ContextCompat.getColor(context, it.timeStampDividerColor)
            }
                ?: ContextCompat.getColor(context, R.color.samsung_note_timestamp_divider_color_for_light),
            timeStampTextColor = themeOverride?.let {
                ContextCompat.getColor(context, it.timeStampTextColor)
            } ?: ContextCompat.getColor(context, R.color.samsung_note_details_color_for_light)
        )

        if (NotesLibrary.getInstance().experimentFeatureFlags.samsungNoteHtmlRenderingEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                noteWebView.settings.forceDark =
                    if (themeOverride?.forceDarkModeInContentHTML == true)
                        WebSettings.FORCE_DARK_ON
                    else WebSettings.FORCE_DARK_OFF
            }
        }
    }

    private fun setSamsungNoteColor(
        contentBg: Int,
        titleColor: Int,
        detailsColor: Int,
        timeStampDividerColor: Int,
        timeStampTextColor: Int
    ) {
        getNoteContainerLayout().setBackgroundResource(contentBg)
        samsungNoteTitle.setTextColor(titleColor)
        timestampDividerTop.setBackgroundColor(timeStampDividerColor)
        detailsColor.apply {
            samsungNoteLabel.setTextColor(this)
        }
        timeStampTextColor.apply {
            timestampText.setTextColor(this)
            samsungNoteLabel.setTextColor(this)
        }
    }
}
