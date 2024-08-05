package com.microsoft.notes.ui.transition

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.FragmentTransaction
import com.microsoft.notes.richtext.editor.styled.NoteStyledView
import com.microsoft.notes.ui.note.edit.EditNoteFragment
import com.microsoft.notes.ui.transition.extensions.accelerateAndDecelerate
import com.microsoft.notes.ui.transition.extensions.getLocationOnScreenAsPoint
import com.microsoft.notes.ui.transition.extensions.nextLayoutListener
import com.microsoft.notes.ui.transition.extensions.onEnd
import com.microsoft.notes.ui.transition.extensions.setLayoutParamsFrom

/**
 * Transitions to the new note edit view without performing animations
 */
fun transitEditViewForNewNoteWithoutAnimation(editNoteFragment: EditNoteFragment, txn: FragmentTransaction) {
    txn.runOnCommit { editNoteFragment.onNavigateToTransitionCompleted() }
}

/**
 * animationOverlay needs to be passed in to use this animation.
 * animationOverlay - FrameLayout be placed in the main activity layout and covering the area of notes list
 */
fun animateEditViewForNewNote(
    animationOverlay: FrameLayout,
    context: Context,
    editNoteFragment: EditNoteFragment
) {
    editNoteFragment.addFirstLayoutListener {
        val noteStyledView = editNoteFragment.findNoteStyledView()
        noteStyledView?.let {
            val editNoteOverlay = createEditNoteOverlay(animationOverlay, context, noteStyledView)
            noteStyledView.visibility = View.INVISIBLE

            animationOverlay.nextLayoutListener {
                editNoteOverlay?.let {
                    editNoteOverlay.translationY = editNoteOverlay.height.toFloat()
                    moveEditNoteAnimator(editNoteOverlay).onEnd {
                        animationOverlay.removeAllViews()
                        noteStyledView.visibility = View.VISIBLE
                        editNoteFragment.onNavigateToTransitionCompleted()
                    }.start()
                }
            }
        }
    }
}

private fun createEditNoteOverlay(
    animationOverlay: FrameLayout,
    context: Context,
    noteStyledView: NoteStyledView
): ImageView? {
    if (noteStyledView.measuredHeight <= 0 || noteStyledView.measuredWidth <= 0) {
        return null
    }
    val imageView = ImageView(context)
    val bitmap = Bitmap.createBitmap(
        noteStyledView.measuredWidth, noteStyledView.measuredHeight, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    noteStyledView.draw(canvas)
    val overlayPosition = animationOverlay.getLocationOnScreenAsPoint()
    imageView.setImageDrawable(BitmapDrawable(context.resources, bitmap))
    animationOverlay.addView(imageView)
    imageView.setLayoutParamsFrom(noteStyledView, overlayPosition)
    return imageView
}

private fun moveEditNoteAnimator(view: View) =
    ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, view.height.toFloat(), 0f).apply {
        duration = NEW_NOTE_ENTER_DURATION
        accelerateAndDecelerate()
    }
