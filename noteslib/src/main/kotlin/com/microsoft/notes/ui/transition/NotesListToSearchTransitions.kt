package com.microsoft.notes.ui.transition

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.noteslist.NotesListComponent
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent
import com.microsoft.notes.ui.search.SearchColorPicker
import com.microsoft.notes.ui.transition.extensions.accelerate
import com.microsoft.notes.ui.transition.extensions.accelerateOrDecelerate
import com.microsoft.notes.ui.transition.extensions.decelerate

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
object NotesListToSearchTransitions {

    /**
     * Animates between list fragment and search fragment.
     */
    fun searchEnterExit(context: Context, entering: Boolean) =
        ScaleTranslateAndFadeTransition(
            dy = -context.resources.getDimension(R.dimen.sn_color_filter_translate_animation)
        )
            .apply {
                addTarget(SearchColorPicker::class.java)
                addTarget(NotesListComponent::class.java)
                duration = SEARCH_CONTENT_FADE_DURATION
                startDelay = searchContentFadeDelay(entering)
                accelerateOrDecelerate(accelerate = !entering)
            }

    /**
     * Animates between list fragment and search fragment.
     */
    fun notesViewEnterExit(context: Context, entering: Boolean) =
        when {
            entering -> slideAndFadeIn(context)
            else -> shrinkAndSlideOut(context)
        }.apply {
            addTarget(NoteItemComponent::class.java)
            duration = NOTES_LIST_FADE_DURATION
            startDelay = notesListFadeDelay(entering)
        }

    private fun slideAndFadeIn(context: Context) =
        ScaleTranslateAndFadeTransition(
            dy = context.resources.getDimension(R.dimen.sn_color_filter_translate_animation)
        ).decelerate()

    private fun shrinkAndSlideOut(context: Context) =
        ScaleTranslateAndFadeTransition(
            dy = context.resources.getDimension(R.dimen.sn_color_filter_translate_animation), scale = .85f
        )
            .accelerate()

    private fun searchContentFadeDelay(entering: Boolean) = if (entering) SEARCH_FADE_DELAY else 0L

    private fun notesListFadeDelay(entering: Boolean) = if (entering) SEARCH_FADE_DELAY else 0L
}
