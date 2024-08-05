package com.microsoft.notes.sampleapp

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.microsoft.notes.models.Note
import com.microsoft.notes.ui.note.edit.EditNoteFragment
import com.microsoft.notes.ui.noteslist.NotesListFragment
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent
import com.microsoft.notes.ui.search.FeedSearchFragment
import com.microsoft.notes.ui.search.SearchFragment
import com.microsoft.notes.ui.transition.NotesListToEditNoteTransitions
import com.microsoft.notes.ui.transition.NotesListToSearchTransitions
import com.microsoft.notes.ui.transition.animateEditViewForNewNote
import com.microsoft.notes.ui.transition.transitEditViewForNewNoteWithoutAnimation
import kotlinx.android.synthetic.main.main_layout.*

val compatibleTransitionCoordinator: TransitionCoordinator by lazy {
    TransitionCoordinatorApi21()
}

open class TransitionCoordinator(private val defaultTransition: Int) {

    private fun default(txn: FragmentTransaction) {
        txn.setTransition(defaultTransition)
    }

    open fun notesListToEditNote(activity: FragmentActivity, from: NotesListFragment, to: EditNoteFragment,
                                 txn: FragmentTransaction, note: Note?) {
        default(txn)
        transitEditViewForNewNoteWithoutAnimation(to, txn)
    }

    open fun editNoteToNotesList(activity: FragmentActivity, from: EditNoteFragment, to: NotesListFragment,
                                 txn: FragmentTransaction) {
        default(txn)
    }

    open fun searchToEditNote(activity: FragmentActivity, from: SearchFragment, to: EditNoteFragment,
                              txn: FragmentTransaction, note: Note?) {
        default(txn)
    }

    open fun editNoteToSearch(activity: FragmentActivity, from: EditNoteFragment, to: SearchFragment,
                              txn: FragmentTransaction) {
        default(txn)
    }

    open fun editNoteToFeedSearch(activity: FragmentActivity, from: EditNoteFragment, to: FeedSearchFragment,
                              txn: FragmentTransaction) {
        default(txn)
    }

    open fun notesListToSearch(activity: FragmentActivity, from: NotesListFragment, to: SearchFragment,
                               txn: FragmentTransaction) {
        default(txn)
    }

    open fun notesListToSearch(activity: FragmentActivity, from: NotesListFragment, to: FeedSearchFragment,
                               txn: FragmentTransaction) {
        default(txn)
    }

    open fun searchToNotesList(activity: FragmentActivity, from: SearchFragment, to: NotesListFragment,
                               txn: FragmentTransaction) {
        default(txn)
    }

    open fun searchToNotesList(activity: FragmentActivity, from: FeedSearchFragment, to: NotesListFragment,
                               txn: FragmentTransaction) {
        default(txn)
    }
}

class TransitionCoordinatorApi21 : TransitionCoordinator(FragmentTransaction.TRANSIT_FRAGMENT_FADE) {
    override fun notesListToEditNote(activity: FragmentActivity, from: NotesListFragment, to: EditNoteFragment,
                                     txn: FragmentTransaction, note: Note?) {
        val isNewNote = NotesListToEditNoteTransitions.prepareListToNote(txn, from.findNotesList(), note)

        from.exitTransition = NotesListToEditNoteTransitions.notesListEnterExit(
                from.findNotesList()?.findRecyclerView(), isNewNote, false)
        from.enterTransition = null
        to.enterTransition = NotesListToEditNoteTransitions.editNoteEnterExit(isEntering = true, isEmpty = false)
        to.exitTransition = null
        to.sharedElementEnterTransition = NotesListToEditNoteTransitions.sharedElements(true)

        if (isNewNote) {
            animateEditViewForNewNote(activity.animationOverlay, activity, to)
        }
    }

    override fun editNoteToNotesList(activity: FragmentActivity, from: EditNoteFragment, to: NotesListFragment,
                                     txn: FragmentTransaction) {
        NotesListToEditNoteTransitions.prepareEditNoteToNotesList(from, to, txn)

        from.exitTransition = NotesListToEditNoteTransitions.editNoteEnterExit(isEntering = false,
                isEmpty = from.isEmpty())
        from.enterTransition = null
        to.enterTransition = NotesListToEditNoteTransitions.notesListEnterExit(
                to.findNotesList()?.findRecyclerView(), false, true)
        to.exitTransition = null
        to.sharedElementEnterTransition = NotesListToEditNoteTransitions.sharedElements(false)
    }

    override fun searchToEditNote(activity: FragmentActivity, from: SearchFragment, to: EditNoteFragment,
                                  txn: FragmentTransaction, note: Note?) {
        NotesListToEditNoteTransitions.prepareListToNote(txn, from.findNotesList(), note)

        from.exitTransition = NotesListToEditNoteTransitions.notesListEnterExit(
                from.findNotesList()?.findRecyclerView(), false, false)
        from.enterTransition = null
        to.enterTransition = NotesListToEditNoteTransitions.editNoteEnterExit(isEntering = true, isEmpty = false)
        to.exitTransition = null
        to.sharedElementEnterTransition = NotesListToEditNoteTransitions.sharedElements(true)
    }

    override fun editNoteToSearch(activity: FragmentActivity, from: EditNoteFragment, to: SearchFragment,
                                  txn: FragmentTransaction) {
        //prepare
        val isEmpty = from.isEmpty()

        to.findNotesList()?.prepareForReturnTransition()

        if (!isEmpty) {
            from.prepareSharedElements { view, name ->
                view.transitionName = name
                txn.addSharedElement(view, name)
            }
        }

        to.addFirstLayoutListener {
            to.findNotesList()?.let { notesListComponent ->
                val noteView: NoteItemComponent? = if (!isEmpty) to.findCurrentNoteView()?.noteView else null
                if (noteView != null) {
                    NotesListToEditNoteTransitions.params.setFromViews(noteView, notesListComponent)
                }
            }
        }

        from.exitTransition = NotesListToEditNoteTransitions.editNoteEnterExit(isEntering = false,
                isEmpty = from.isEmpty())
        from.enterTransition = null
        to.enterTransition = NotesListToEditNoteTransitions.notesListEnterExit(
                to.findNotesList()?.findRecyclerView(), false, true)
        to.exitTransition = null
        to.sharedElementEnterTransition = NotesListToEditNoteTransitions.sharedElements(false)
    }

    override fun notesListToSearch(activity: FragmentActivity, from: NotesListFragment, to: SearchFragment,
                                   txn: FragmentTransaction) {
        from.exitTransition = NotesListToSearchTransitions.notesViewEnterExit(activity, entering = false)
        from.enterTransition = null
        to.enterTransition = NotesListToSearchTransitions.searchEnterExit(activity, true)
        to.exitTransition = null
        to.sharedElementEnterTransition = null
    }

    override fun notesListToSearch(activity: FragmentActivity, from: NotesListFragment, to: FeedSearchFragment,
                                   txn: FragmentTransaction) {
        from.exitTransition = NotesListToSearchTransitions.notesViewEnterExit(activity, entering = false)
        from.enterTransition = null
        to.enterTransition = NotesListToSearchTransitions.searchEnterExit(activity, true)
        to.exitTransition = null
        to.sharedElementEnterTransition = null
    }

    override fun searchToNotesList(activity: FragmentActivity, from: SearchFragment, to: NotesListFragment,
                                   txn: FragmentTransaction) {
        from.enterTransition = null
        from.exitTransition = NotesListToSearchTransitions.searchEnterExit(activity, false)
        to.enterTransition = NotesListToSearchTransitions.notesViewEnterExit(activity, entering = true)
        to.exitTransition = null
        to.sharedElementEnterTransition = null
    }

    override fun searchToNotesList(activity: FragmentActivity, from: FeedSearchFragment, to: NotesListFragment,
                                   txn: FragmentTransaction) {
        from.enterTransition = null
        from.exitTransition = NotesListToSearchTransitions.searchEnterExit(activity, false)
        to.enterTransition = NotesListToSearchTransitions.notesViewEnterExit(activity, entering = true)
        to.exitTransition = null
        to.sharedElementEnterTransition = null
    }
}
