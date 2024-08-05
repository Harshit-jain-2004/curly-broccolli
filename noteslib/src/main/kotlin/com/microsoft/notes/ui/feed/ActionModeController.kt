package com.microsoft.notes.ui.feed

import android.app.Activity
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.extensions.sendAccessibilityAnnouncement

class ActionModeController : ActionMode.Callback {
    private var actionMode: ActionMode? = null
    var activity: Activity
    val actionModeClickListener: ActionModeClickListener
    val callback: Callback
    val closeActionModeButton = R.id.action_mode_close_button // referring to id defined in Android' Layout for close Button
    var isActionModeActive: Boolean = false
        private set

    constructor(
        activity: Activity,
        actionModeClickListener: ActionModeClickListener,
        callback: Callback
    ) {
        this.activity = activity
        this.actionModeClickListener = actionModeClickListener
        this.callback = callback
    }

    fun startActionMode() {
        isActionModeActive = true
        callback.onStartActionMode()
        actionMode = activity.startActionMode(this)
        activity.findViewById<View>(closeActionModeButton).contentDescription = activity.getString(R.string.close)
        activity.applicationContext?.sendAccessibilityAnnouncement(activity.getString(R.string.sn_entering_Selection))
    }

    fun finishActionMode() {
        actionMode?.finish()
    }

    fun invalidateActionMenu() {
        actionMode?.invalidate()
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = actionModeClickListener.onActionItemClicked(item)

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.sn_feed_actionmode_menu, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu ?: return false
        mode?.title = callback.getActionModeTitle()
        callback.onPrepareActionMode(menu)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        isActionModeActive = false
        actionMode = null
        callback.onFinishActionMode()
        activity.applicationContext?.sendAccessibilityAnnouncement(activity.getString(R.string.sn_exiting_Selection))
    }
}

interface ActionModeClickListener {
    fun onActionItemClicked(menuItem: MenuItem?): Boolean
}

interface Callback {
    fun onStartActionMode() {}
    fun onFinishActionMode() {}
    fun onPrepareActionMode(menu: Menu): Boolean = false
    fun getActionModeTitle(): String = ""
}
