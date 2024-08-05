package com.microsoft.notes.ui

interface LifecycleCallbacks {

    var shouldHandleStateUpdates: Boolean
    var areListenersAdded: Boolean

    fun onStart()
    fun onResume()
    fun onPause()
    fun onStop()
    fun onDestroy()
}
