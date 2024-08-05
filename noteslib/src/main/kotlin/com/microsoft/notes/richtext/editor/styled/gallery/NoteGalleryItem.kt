package com.microsoft.notes.richtext.editor.styled.gallery

import android.graphics.drawable.DrawableContainer.DrawableContainerState
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.getFontColor
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.editor.styled.loadRoundedImageFromUri
import com.microsoft.notes.richtext.editor.styled.toContextColor
import com.microsoft.notes.richtext.editor.styled.toMediumContextColor
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.ui.note.ink.NotesEditInkCallback
import kotlinx.android.synthetic.main.sn_note_gallery_item_ink.view.*
import kotlinx.android.synthetic.main.sn_note_gallery_item_single_image.view.noteGalleryItemImageContainer

sealed class NoteGalleryItem(view: View) : RecyclerView.ViewHolder(view) {
    class NoteGalleryInk(val view: View) : NoteGalleryItem(view) {
        fun setInk(document: Document, revision: Long, inkColorId: Int) {
            itemView.noteGalleryItemInkView.setDocumentAndUpdateScaleFactor(document)
            itemView.noteGalleryItemInkView.revision = revision
            itemView.noteGalleryItemInkView.inkPaint.color = ContextCompat.getColor(view.context, inkColorId)
        }
        fun setInkCallback(inkCallback: NotesEditInkCallback) {
            itemView.noteGalleryItemInkView.setNotesEditInkViewCallback(inkCallback)
        }
    }

    abstract class NoteGalleryImage(view: View) : NoteGalleryItem(view) {
        abstract fun setMedia(media: Media, selected: Boolean, noteColor: Color, callback: NoteGalleryAdapter.Callback?)
        abstract fun setSelected(selected: Boolean, noteColor: Color)

        protected fun setMedia(
            imageBackground: View,
            imageView: ImageView,
            media: Media,
            selected: Boolean,
            noteColor: Color,
            callback: NoteGalleryAdapter.Callback?,
            centerCrop: Boolean
        ) {
            imageView.loadRoundedImageFromUri(media.localUrl, centerCrop = centerCrop)
            val imageAttachmentString = itemView.context.getString(R.string.sn_image_attachment)
            imageView.contentDescription =
                if (media.altText != null && media.altText.isNotEmpty()) {
                    "${media.altText} $imageAttachmentString"
                } else {
                    imageAttachmentString
                }

            setSelected(selected, noteColor)
            setFocusColor(imageBackground, noteColor)
            var floatingContextMenu: View? = null

            itemView.setOnClickListener {
                if (!NotesLibrary.getInstance().experimentFeatureFlags.enableScanButtonInImage || !NotesLibrary.getInstance().isRestricted()) {
                    callback?.displayFullScreenImage(media)
                } else {
                    val layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    val constraintLayout = itemView.noteGalleryItemImageContainer
                    if (floatingContextMenu == null) {
                        floatingContextMenu = LayoutInflater.from(itemView.context).inflate(R.layout.floating_context_menu, null)
                        constraintLayout.addView(floatingContextMenu, layoutParams)
                    }
                    floatingContextMenu?.visibility = View.VISIBLE

                    floatingContextMenu?.findViewById<TextView>(R.id.open_note)?.setOnClickListener {
                        floatingContextMenu?.visibility = View.GONE
                        setSelected(false, noteColor)
                        callback?.displayFullScreenImage(media)
                    }
                    floatingContextMenu?.findViewById<TextView>(R.id.scan_note)?.setOnClickListener {
                        floatingContextMenu?.visibility = View.GONE
                        setSelected(false, noteColor)
                        NotesLibrary.getInstance().sendScanButtonClickedAction()
                    }
                    floatingContextMenu?.findViewById<TextView>(R.id.delete_note)?.setOnClickListener {
                        floatingContextMenu?.visibility = View.GONE
                        setSelected(false, noteColor)
                        callback?.deleteMedia(media)
                    }
                }
            }
        }

        protected fun setSelected(view: View, selected: Boolean, noteColor: Color) {
            if (selected) {
                val drawable = view.background as GradientDrawable
                val strokeWidth = itemView.resources.getDimensionPixelSize(R.dimen.sn_image_overlay_stroke)
                drawable.setStroke(strokeWidth, noteColor.toMediumContextColor(itemView.context))
                view.background = drawable
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        }

        private fun setFocusColor(imageBackground: View, noteColor: Color) {
            val drawable = imageBackground.background as StateListDrawable
            val drawableContainerState = drawable.constantState as DrawableContainerState
            val children = drawableContainerState.children
            val stroke = children[0] as GradientDrawable
            val strokeWidth = itemView.resources.getDimensionPixelSize(R.dimen.sn_image_background_stroke)
            stroke.setStroke(strokeWidth, noteColor.getFontColor().toContextColor(itemView.context))
            imageBackground.background = drawable
        }
    }
}
