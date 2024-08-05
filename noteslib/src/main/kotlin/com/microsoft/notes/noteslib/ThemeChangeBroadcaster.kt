package com.microsoft.notes.noteslib

import android.view.View
import com.microsoft.notes.ui.theme.extensions.setTheme

/**
 * Views that are themed and want to listen to changes in theme should be registered in this class. If a new
 * theme is set, all current listeners will be themed accordingly.
 */
class ThemeChangeBroadcaster {
    private inner class ThemeChangeListener(private val broadcaster: ThemeChangeBroadcaster) :
        View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View?) {
            view?.let {
                broadcaster.listeners.add(it)
                it.setTheme(it.context, currentTheme)
            }
        }
        override fun onViewDetachedFromWindow(view: View?) {
            view?.let { broadcaster.listeners.remove(it) }
        }
    }

    /**
     * Setting this value will broadcast the new value to all listening views
     */
    var currentTheme: NotesThemeOverride
        set(value) {
            field = value
            broadcastThemeChange(value)
        }

    private val listeners: MutableSet<View> = mutableSetOf()
    private val onAttachStateChangeListener: ThemeChangeListener

    constructor(theme: NotesThemeOverride) {
        currentTheme = theme
        onAttachStateChangeListener = ThemeChangeListener(this)
    }

    /**
     * Should be called in the View's init block, i.e., before the view is actually attached. This will style the
     * View according to its android:tag attribute with setTheme, as well as listen to incoming theme changes for
     * as long as the view is attached.
     */
    fun registerAndSetTheme(listener: View) {
        listener.setTheme(listener.context, currentTheme)
        listener.addOnAttachStateChangeListener(onAttachStateChangeListener)
    }

    private fun broadcastThemeChange(newTheme: NotesThemeOverride) {
        listeners.forEach { it.setTheme(it.context, newTheme) }
    }
}
