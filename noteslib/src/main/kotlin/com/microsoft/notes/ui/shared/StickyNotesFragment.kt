package com.microsoft.notes.ui.shared

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary

open class StickyNotesFragment : Fragment() {
    init {
        allowEnterTransitionOverlap = true
        allowReturnTransitionOverlap = true
    }

    private var currentNoteId: String? = null

    private val firstLayoutListeners = mutableListOf<(View) -> Unit>()

    @Suppress("UnsafeCallOnNullableType")
    private val listener: ViewTreeObserver.OnGlobalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            view?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
            firstLayoutListeners.forEach { it(this@StickyNotesFragment.view!!) }
            firstLayoutListeners.clear()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    protected fun getCurrentNoteId(): String? = currentNoteId

    open fun setCurrentNoteId(currentNoteId: String) {
        this.currentNoteId = currentNoteId
    }

    protected open fun getCurrentNote(): Note? {
        val safeCurrentNoteId = currentNoteId
        return if (safeCurrentNoteId != null) {
            NotesLibrary.getInstance().getNoteById(safeCurrentNoteId)
        } else {
            null
        }
    }

    fun addFirstLayoutListener(listener: (View) -> Unit) {
        firstLayoutListeners.add(listener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        view?.viewTreeObserver?.removeOnGlobalLayoutListener(listener)
    }

    // Transitions
    fun tryHidingSoftKeyboard() {
        val focusedView: View? = activity?.currentFocus
        if (focusedView != null) {
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val rootView = activity?.window?.decorView?.rootView
            imm.hideSoftInputFromWindow(rootView?.windowToken, 0)
        }
    }
}
