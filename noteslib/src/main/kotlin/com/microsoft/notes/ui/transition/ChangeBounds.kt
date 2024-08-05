package com.microsoft.notes.ui.transition

import android.transition.ChangeBounds

class ChangeBounds(private val params: DynamicTransitionParams) : ChangeBounds() {
    override fun getDuration(): Long = params.duration
}
