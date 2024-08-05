package com.microsoft.notes.ui.transition

import android.annotation.TargetApi
import android.os.Build
import android.transition.Transition
import android.transition.TransitionSet
import android.view.Gravity
import android.view.View
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.microsoft.notes.models.Note
import com.microsoft.notes.ui.note.edit.EditNoteFragment
import com.microsoft.notes.ui.noteslist.NotesListComponent
import com.microsoft.notes.ui.noteslist.NotesListFragment
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent
import com.microsoft.notes.ui.transition.extensions.accelerateAndDecelerate
import com.microsoft.notes.ui.transition.extensions.addTargets
import com.microsoft.notes.ui.transition.extensions.addTransitions
import com.microsoft.notes.ui.transition.extensions.decelerate
import com.microsoft.notes.ui.transition.extensions.getBoundsOnScreen
import com.microsoft.notes.ui.transition.extensions.withSidePropagation

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
object NotesListToEditNoteTransitions {
    val params = DynamicTransitionParams()

    fun prepareEditNoteToNotesList(from: EditNoteFragment, to: NotesListFragment, txn: FragmentTransaction) {
        val isEmpty = from.isEmpty()
        val notesList: NotesListComponent? = to.findNotesList()
        notesList?.prepareForReturnTransition()
        if (!isEmpty) {
            from.prepareSharedElements { view, name ->
                view.transitionName = name
                txn.addSharedElement(view, name)
            }
        }

        to.addFirstLayoutListener {
            if (!isEmpty) {
                to.ensureCurrentEditNoteVisibility()
            }
            val noteView: NoteItemComponent? = if (!isEmpty) to.findCurrentNoteView()?.noteView else null
            notesList?.let { notesList ->
                if (noteView != null) {
                    params.setFromViews(noteView, notesList as View)
                }
            }
        }
    }

    fun prepareListToNote(
        txn: FragmentTransaction,
        notesList: NotesListComponent?,
        note: Note?
    ): Boolean {
        var noteView: NoteItemComponent? = null
        if (note != null && !note.isEmpty) {
            noteView = notesList?.prepareForEditNoteTransition(note, txn)
        }
        val isNewNote = noteView == null
        if (!isNewNote) {
            params.setFromViews(noteView as View, notesList as View)
        } else {
            notesList?.prepareForNewNoteTransition()
        }

        return isNewNote
    }

    /**
     * NotesListToEditNoteTransitions.prepareListToNote  or NotesListToEditNoteTransitions.prepareEditNoteToNotesList
     * should be called first
     */
    fun notesListEnterExit(recyclerView: RecyclerView?, isNewNote: Boolean, isEntering: Boolean):
        TransitionSet {
        return TransitionSet().addTransitions(
            when {
                isNewNote && !isEntering -> slideNotesUnder(recyclerView)
                else -> slideNotesInOrOut()
            }
        )
    }

    /**
     * NotesListToEditNoteTransitions.prepareListToNote  or NotesListToEditNoteTransitions.prepareEditNoteToNotesList
     * should be called first
     */
    fun sharedElements(toEditNote: Boolean): DynamicDurationTransitionSet {
        with(DynamicDurationTransitionSet(params)) {
            addTransitions(
                ChangeBounds(params).addTargets("card", "linearLayout"),
                BodyTextTransition(params, toEditNote).addTarget("body")
            )

            return this
        }
    }

    /**
     * NotesListToEditNoteTransitions.prepareListToNote or NotesListToEditNoteTransitions.prepareEditNoteToNotesList
     * should be called first
     */
    fun editNoteEnterExit(isEntering: Boolean, isEmpty: Boolean): TransitionSet =
        TransitionSet().apply {
            if (!isEntering && isEmpty) {
                addTransition(fadeEmptyNoteOnExit())
            }
        }

    private fun slideNotesUnder(recyclerView: RecyclerView?): Transition {
        val recyclerViewBounds = recyclerView?.getBoundsOnScreen()
        return ScaleTranslateAndFadeTransition(
            dy = 70f, scale = .85f,
            clipBounds = recyclerViewBounds
        ).apply {
            addTarget(NoteItemComponent::class.java)
            duration = SLIDE_NOTES_UNDER_DURATION
            withSidePropagation(Gravity.BOTTOM, 2f)
            accelerateAndDecelerate()
        }
    }

    private fun slideNotesInOrOut(): Transition =
        DynamicDurationTransitionSet(params).addTransitions(
            VerticalSlideWithCompression(Gravity.TOP, params).addTarget("up"),
            VerticalSlideWithCompression(Gravity.BOTTOM, params).addTarget("down")
        )

    private fun fadeEmptyNoteOnExit(): Transition =
        Fade(params).addTarget("card").decelerate()
}
