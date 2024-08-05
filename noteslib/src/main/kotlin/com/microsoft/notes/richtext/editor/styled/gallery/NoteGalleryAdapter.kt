package com.microsoft.notes.richtext.editor.styled.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Media
import com.microsoft.notes.noteslib.NotesThemeOverride
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.editor.NotesEditText.Companion.UNINITIALIZED_REV_VAL
import com.microsoft.notes.richtext.editor.extensions.toInkColorNightResource
import com.microsoft.notes.richtext.editor.extensions.toInkColorResource
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.ui.note.ink.NotesEditInkCallback

private const val LATEST = 0
private const val SQUARE = 1
private const val SINGLE_IMAGE = 2
private const val INK = 3
private const val SAMSUNG_PREVIEW_IMAGE = 4
internal const val ITEMS_IN_A_ROW = 2

class NoteGalleryAdapter : RecyclerView.Adapter<NoteGalleryItem>() {
    interface Callback {
        fun displayFullScreenImage(media: Media)
        fun editAltText(media: Media) {}
        fun deleteMedia(media: Media) {}
    }

    private var media = listOf<Media>()
    private var document = Document()
    private var callback: Callback? = null
    private var selectedItem = -1
    private var noteColor = Color.getDefault()
    private var contextualMenuEnabled = false
    private var themeOverride: NotesThemeOverride.StickyNoteCanvasThemeOverride? = null
    private var revision = UNINITIALIZED_REV_VAL
    private var inkCallback: NotesEditInkCallback? = null

    fun setThemeOverride(theme: NotesThemeOverride.StickyNoteCanvasThemeOverride?) {
        if (this.themeOverride == theme) return
        themeOverride = theme
        notifyDataSetChanged()
    }

    fun setNotesEditInkViewCallback(inkCallback: NotesEditInkCallback) {
        this.inkCallback = inkCallback
    }

    fun setMedia(media: List<Media>, noteColor: Color, contextualMenuEnabled: Boolean) {
        if (this.media == media && this.noteColor == noteColor &&
            this.contextualMenuEnabled == contextualMenuEnabled
        ) {
            return
        }

        this.media = media
        this.noteColor = noteColor
        this.contextualMenuEnabled = contextualMenuEnabled
        notifyDataSetChanged()
    }

    fun setInk(document: Document, revision: Long) {
        if (this.document == document) {
            return
        }
        this.document = document
        if (this.revision == UNINITIALIZED_REV_VAL)
            this.revision = revision

        notifyDataSetChanged()
    }

    fun setDocument(document: Document) {
        this.document = document
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteGalleryItem {
        val layout = when (viewType) {
            LATEST -> R.layout.sn_note_gallery_item_latest
            SQUARE -> R.layout.sn_note_gallery_item_square
            SINGLE_IMAGE -> R.layout.sn_note_gallery_item_single_image
            INK -> R.layout.sn_note_gallery_item_ink
            SAMSUNG_PREVIEW_IMAGE -> R.layout.samsung_gallery_item_preview_image
            else -> throw IllegalStateException("Unknown NoteGalleryItem type: $viewType")
        }

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return when (viewType) {
            LATEST -> NoteGalleryItemLatest(view)
            SQUARE -> NoteGalleryItemSquare(view)
            SINGLE_IMAGE -> NoteGalleryItemSingleImage(view)
            INK -> NoteGalleryItem.NoteGalleryInk(view)
            SAMSUNG_PREVIEW_IMAGE -> NoteGalleryItemSamsungPreviewImage(view)
            else -> throw IllegalStateException("Unknown NoteGalleryItem type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: NoteGalleryItem, position: Int) {
        when (holder) {
            is NoteGalleryItem.NoteGalleryInk -> {
                inkCallback?.let { holder.setInkCallback(it) }

                val theme = themeOverride
                if (theme != null) {
                    holder.setInk(document, revision, noteColor.toInkColorNightResource())
                } else {
                    holder.setInk(document, revision, noteColor.toInkColorResource())
                }
            }
            is NoteGalleryItem.NoteGalleryImage -> {
                val displayedMedia = media[position]
                holder.setMedia(displayedMedia, selectedItem == position, noteColor, callback)
                if (contextualMenuEnabled) {
                    holder.itemView.setOnCreateContextMenuListener { menu, _, _ ->
                        val addAltTextMenuItem = menu.add(
                            holder.itemView.context.getString(
                                if (!displayedMedia.altText.isNullOrEmpty()) {
                                    R.string.sn_contextual_menu_image_alt_text_edit
                                } else {
                                    R.string.sn_contextual_menu_image_alt_text_add
                                }
                            )
                        )
                        addAltTextMenuItem.setOnMenuItemClickListener {
                            callback?.editAltText(displayedMedia)
                            menuDismissed()
                            true
                        }

                        val deleteMenuItem = menu.add(
                            holder.itemView.context.getString(
                                R.string.sn_contextual_menu_image_delete
                            )
                        )
                        deleteMenuItem.setOnMenuItemClickListener {
                            callback?.deleteMedia(displayedMedia)
                            menuDismissed()
                            true
                        }

                        selectedItem = holder.adapterPosition
                        holder.setSelected(true, noteColor)
                    }
                }
            }
        }
    }

    fun menuDismissed() {
        if (selectedItem != -1) {
            selectedItem = -1
            notifyDataSetChanged()
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (document.isInkDocument) return INK
        if (document.isSamsungNoteDocument) return SAMSUNG_PREVIEW_IMAGE

        if (itemCount % ITEMS_IN_A_ROW == 0) {
            return SQUARE
        }

        if (itemCount == 1) {
            return SINGLE_IMAGE
        }

        return if (position == 0) LATEST else SQUARE
    }

    override fun getItemCount(): Int = if (document.isInkDocument) 1 else media.size
}
