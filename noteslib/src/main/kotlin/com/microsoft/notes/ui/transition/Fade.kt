package com.microsoft.notes.ui.transition

import android.transition.Fade

class Fade(private val params: DynamicTransitionParams) : Fade() {
    override fun getDuration(): Long = params.duration
}
